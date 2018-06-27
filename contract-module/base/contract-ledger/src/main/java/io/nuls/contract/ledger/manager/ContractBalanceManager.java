/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.contract.ledger.manager;

import io.nuls.account.service.AccountService;
import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.ledger.module.ContractBalance;
import io.nuls.contract.ledger.util.ContractLedgerUtil;
import io.nuls.contract.storage.service.ContractAddressStorageService;
import io.nuls.contract.storage.service.ContractUtxoStorageService;
import io.nuls.core.tools.array.ArraysTool;
import io.nuls.core.tools.log.Log;
import io.nuls.core.tools.param.AssertUtil;
import io.nuls.db.model.Entry;
import io.nuls.db.service.BatchOperation;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Component;
import io.nuls.kernel.model.Address;
import io.nuls.kernel.model.Coin;
import io.nuls.kernel.model.Na;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.utils.AddressTool;
import io.nuls.kernel.utils.NulsByteBuffer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/6/7
 */
@Component
public class ContractBalanceManager {

    @Autowired
    private ContractUtxoStorageService contractUtxoStorageService;

    @Autowired
    private ContractAddressStorageService contractAddressStorageService;

    @Autowired
    private AccountService accountService;

    private Map<String, ContractBalance> balanceMap = new ConcurrentHashMap<>();

    private Map<String, ContractBalance> tempBalanceMap;

    private Lock lock = new ReentrantLock();

    public Map<String, ContractBalance> getTempBalanceMap() {
        return tempBalanceMap;
    }

    public void setTempBalanceMap(Map<String, ContractBalance> tempBalanceMap) {
        this.tempBalanceMap = tempBalanceMap;
    }

    /**
     * 初始化缓存本地所有合约账户的余额信息
     */
    public void initContractBalance() {
        balanceMap.clear();
        List<Coin> coinList = new ArrayList<>();
        List<Entry<byte[], byte[]>> rawList = contractUtxoStorageService.loadAllCoinList();
        Coin coin;
        String strAddress;
        ContractBalance balance;
        byte[] fromOwner;
        for (Entry<byte[], byte[]> coinEntry : rawList) {
            coin = new Coin();
            try {
                coin.parse(coinEntry.getValue(), 0);
                strAddress = asString(coin.getOwner());
            } catch (NulsException e) {
                Log.error("parse contract coin error form db", e);
                continue;
            }
            balance = balanceMap.get(strAddress);
            if(balance == null) {
                balance = new ContractBalance();
                balanceMap.put(strAddress, balance);
            }
            if (coin.usable()) {
                balance.addUsable(coin.getNa());
            } else {
                balance.addLocked(coin.getNa());
            }
        }
    }

    private String asString(byte[] bytes) {
        AssertUtil.canNotEmpty(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 不删数据库数据
     *
     * @param removeAddress
     * @return
     */
    @Deprecated
    public Result<List<Entry<byte[], byte[]>>> removeBalance(byte[] removeAddress) {
        lock.lock();
        try {
            if(!ContractLedgerUtil.isContractAddress(removeAddress)) {
                return Result.getFailed(ContractErrorCode.CONTRACT_ADDRESS_NOT_EXIST);
            }

            List<Entry<byte[], byte[]>> rawList = contractUtxoStorageService.loadAllCoinList();
            Coin coin;
            byte[] address;
            String strAddress;
            List<Entry<byte[], byte[]>> deleteList = new ArrayList<>();
            BatchOperation batchOperation = contractUtxoStorageService.createBatchOperation();
            for (Entry<byte[], byte[]> coinEntry : rawList) {
                coin = new Coin();
                try {
                    coin.parse(coinEntry.getValue(), 0);
                } catch (NulsException e) {
                    Log.error("parse contract coin error form db", e);
                    continue;
                }
                address = coin.getOwner();
                if(!Arrays.equals(removeAddress, address)) {
                    continue;
                }
                batchOperation.delete(coinEntry.getKey());
                deleteList.add(coinEntry);
            }
            Result result = batchOperation.executeBatch();
            if(result.isSuccess()) {
                balanceMap.remove(asString(removeAddress));
            }
            return Result.getSuccess().setData(deleteList);
        } finally {
            lock.unlock();
        }
    }
    /**
     * 获取账户余额
     *
     * @param address
     * @return
     */
    public Result<ContractBalance> getBalance(byte[] address) {
        lock.lock();
        try {
            if (address == null || address.length != Address.ADDRESS_LENGTH) {
                return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
            }

            if (!ContractLedgerUtil.isContractAddress(address)) {
                return Result.getFailed(ContractErrorCode.CONTRACT_ADDRESS_NOT_EXIST);
            }

            String addressKey = asString(address);
            ContractBalance balance = null;
            // 打包或验证区块前创建一个临时余额区，实时更新余额，打包完或验证区块后移除
            if(tempBalanceMap != null) {
                balance = tempBalanceMap.get(addressKey);
                if(balance == null) {
                    balance = balanceMap.get(addressKey);
                    if (balance == null) {
                        balance = new ContractBalance();
                        balanceMap.put(addressKey, balance);
                        tempBalanceMap.put(addressKey, balance);
                    }
                }
            } else {
                if (balance == null) {
                    balance = new ContractBalance();
                    balanceMap.put(addressKey, balance);
                }
            }

            return Result.getSuccess().setData(balance);
        } finally {
            lock.unlock();
        }
    }

    public void addTempBalance(byte[] address, long amount) {
        String addressKey = asString(address);
        ContractBalance contractBalance = tempBalanceMap.get(addressKey);

        if(contractBalance != null) {
            contractBalance.addUsable(Na.valueOf(amount));
        }
    }

    public void minusTempBalance(byte[] address, long amount) {
        String addressKey = asString(address);
        ContractBalance contractBalance = tempBalanceMap.get(addressKey);

        if(contractBalance != null) {
            contractBalance.minusUsable(Na.valueOf(amount));
        }
    }



    /**
     * 刷新余额
     *
     * @param address
     */
    public void refreshBalance(List<Entry<byte[], byte[]>> addUtxoList, List<Entry<byte[], byte[]>> deleteUtxoList) {
        lock.lock();
        try {
            Coin coin;
            ContractBalance balance;
            String strAddress;
            if(addUtxoList != null) {
                for (Entry<byte[], byte[]> addUtxo : addUtxoList) {
                    coin = new Coin();
                    try {
                        coin.parse(addUtxo.getValue(), 0);
                    } catch (NulsException e) {
                        Log.error("parse contract coin error form db", e);
                        continue;
                    }
                    strAddress = asString(coin.getOwner());
                    balance = balanceMap.get(strAddress);
                    if(balance == null) {
                        balance = new ContractBalance();
                        balanceMap.put(strAddress, balance);
                    }
                    if (coin.usable()) {
                        balance.addUsable(coin.getNa());
                    } else {
                        //TODO pierre 合约地址的金额不会存在锁定金额，因为金额都是转账转入，锁定有两种，一种是共识锁定，一种是高度锁定，这两种情况都不会发生在合约地址上
                        balance.addLocked(coin.getNa());
                    }
                }
            }

            if(deleteUtxoList != null) {
                for (Entry<byte[], byte[]> deleteUtxo : deleteUtxoList) {
                    coin = new Coin();
                    try {
                        coin.parse(deleteUtxo.getValue(), 0);
                    } catch (NulsException e) {
                        Log.error("parse contract coin error form db", e);
                        continue;
                    }
                    strAddress = asString(coin.getOwner());
                    balance = balanceMap.get(strAddress);
                    if(balance == null) {
                        balance = new ContractBalance();
                        balanceMap.put(strAddress, balance);
                    }
                    if (coin.usable()) {
                        balance.minusUsable(coin.getNa());
                    } else {
                        balance.minusLocked(coin.getNa());
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public List<Coin> getCoinListByAddress(byte[] address) {
        List<Coin> coinList = new ArrayList<>();
        List<Entry<byte[], byte[]>> rawList = contractUtxoStorageService.loadAllCoinList();
        for (Entry<byte[], byte[]> coinEntry : rawList) {
            Coin coin = new Coin();
            try {
                coin.parse(coinEntry.getValue(), 0);
            } catch (NulsException e) {
                Log.info("parse coin form db error");
                continue;
            }
            if (Arrays.equals(coin.getOwner(), address)) {
                coin.setOwner(coinEntry.getKey());
                coinList.add(coin);
            }
        }
        return coinList;
    }
}

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
package io.nuls.contract.ledger.service.impl;

import io.nuls.account.ledger.constant.AccountLedgerErrorCode;
import io.nuls.account.ledger.service.AccountLedgerService;
import io.nuls.account.model.Balance;
import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.ledger.service.ContractUtxoService;
import io.nuls.contract.ledger.util.ContractLedgerUtil;
import io.nuls.contract.storage.service.ContractUtxoStorageService;
import io.nuls.core.tools.array.ArraysTool;
import io.nuls.core.tools.crypto.Hex;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.exception.NulsRuntimeException;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Service;
import io.nuls.kernel.model.*;
import io.nuls.kernel.utils.AddressTool;
import io.nuls.kernel.utils.VarInt;
import io.nuls.ledger.service.LedgerService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/6/5
 */
@Service
public class ContractUtxoServiceImpl implements ContractUtxoService {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private ContractUtxoStorageService contractUtxoStorageService;

    @Autowired
    private AccountLedgerService accountLedgerService;

    /**
     * 两种交易
     *   第一种交易是普通地址转入合约地址
     *
     *   第二种交易是智能合约特殊转账交易，合约地址转出到普通地址/合约地址
     *
     *
     * @param tx
     * @return
     */
    @Override
    public Result saveUtxoForContractAddress(Transaction tx) {
        if (tx == null) {
            return Result.getFailed(KernelErrorCode.NULL_PARAMETER);
        }

        CoinData coinData = tx.getCoinData();

        if (coinData != null) {
            // 在合约独立账本中，只有合约特殊转账交易才能从合约地址中转出资产，所以只有这类交易才处理fromCoinData -> delete - from
            List<byte[]> fromList = new ArrayList<>();
            if(tx.getType() == ContractConstant.TX_TYPE_CONTRACT_TRANSFER) {
                List<Coin> froms = coinData.getFrom();
                byte[] fromSource;
                byte[] utxoFromSource;
                byte[] fromIndex;
                Coin fromOfFromCoin;
                for (Coin from : froms) {
                    fromSource = from.getOwner();
                    utxoFromSource = new byte[tx.getHash().size()];
                    fromIndex = new byte[fromSource.length - utxoFromSource.length];
                    System.arraycopy(fromSource, 0, utxoFromSource, 0, tx.getHash().size());
                    System.arraycopy(fromSource, tx.getHash().size(), fromIndex, 0, fromIndex.length);

                    fromOfFromCoin = from.getFrom();

                    if (fromOfFromCoin == null) {
                        Transaction sourceTx = null;
                        try {
                            sourceTx = ledgerService.getTx(NulsDigestData.fromDigestHex(Hex.encode(fromSource)));
                            //TODO pierre 未确认交易可能已经被删除 BlockServiceImpl.saveBlock, 需要想办法处理
                            //TODO pierre 特殊合约交易是否需要未确认交易, 这类交易在打包/验证区块时执行, 已代表这是确认交易, 不过打包时连续特殊交易如何处理，如何组装CoinData
                            if (sourceTx == null) {
                                //TODO sourceTx = accountLedgerService.getUnconfirmedTransaction(NulsDigestData.fromDigestHex(Hex.encode(fromSource))).getData();
                            }
                        } catch (Exception e) {
                            throw new NulsRuntimeException(e);
                        }
                        if (sourceTx == null) {
                            return Result.getFailed(AccountLedgerErrorCode.SOURCE_TX_NOT_EXSITS);
                        }
                        fromOfFromCoin = sourceTx.getCoinData().getTo().get((int) new VarInt(fromIndex, 0).value);
                    }

                    byte[] address = fromOfFromCoin.getOwner();

                    if (!ContractLedgerUtil.isContractAddress(address)) {
                        continue;
                    }

                    fromList.add(ArraysTool.joinintTogether(address, from.getOwner()));
                }
            }

            // save utxo - to
            List<Coin> tos = coinData.getTo();
            Map<byte[], byte[]> toMap = new HashMap<>();
            byte[] txHashBytes = null;
            try {
                txHashBytes = tx.getHash().serialize();
            } catch (IOException e) {
                throw new NulsRuntimeException(e);
            }
            Coin to;
            byte[] toAddress;
            byte[] outKey;
            for (int i = 0, length = tos.size(); i < length; i++) {
                to = tos.get(i);
                toAddress = to.getOwner();
                if (!ContractLedgerUtil.isContractAddress(toAddress)) {
                    continue;
                }
                try {
                    outKey = ArraysTool.joinintTogether(toAddress, txHashBytes, new VarInt(i).encode());
                    toMap.put(outKey, to.serialize());
                } catch (IOException e) {
                    throw new NulsRuntimeException(e);
                }
            }
            Result result = contractUtxoStorageService.batchSaveAndDeleteUTXO(toMap, fromList);
            if (result.isFailed() || result.getData() == null || (int) result.getData() != toMap.size() + fromList.size()) {
                return Result.getFailed();
            }
        }
        return Result.getSuccess();
    }

    @Override
    public Result saveUtxoForAccount(Transaction tx, byte[] addresses) {
        //TODO pierre 是否需要此方法
        return null;
    }

    @Override
    public Result deleteUtxoOfTransaction(Transaction tx) {
        if (tx == null) {
            return Result.getFailed(KernelErrorCode.NULL_PARAMETER);
        }

        CoinData coinData = tx.getCoinData();
        byte[] txHashBytes = null;
        try {
            txHashBytes = tx.getHash().serialize();
        } catch (IOException e) {
            throw new NulsRuntimeException(e);
        }
        if (coinData != null) {
            // delete utxo - to
            List<Coin> tos = coinData.getTo();
            byte[] indexBytes;
            List<byte[]> toList = new ArrayList<>();
            byte[] outKey;
            Coin to;
            byte[] toAddress;
            for (int i = 0, length = tos.size(); i < length; i++) {
                to = tos.get(i);
                toAddress = to.getOwner();
                if(!ContractLedgerUtil.isContractAddress(toAddress)) {
                    continue;
                }
                outKey = ArraysTool.joinintTogether(toAddress, txHashBytes, new VarInt(i).encode());
                toList.add(outKey);
            }

            // save - from
            Map<byte[], byte[]> fromMap = new HashMap<>();
            if(tx.getType() == ContractConstant.TX_TYPE_CONTRACT_TRANSFER) {
                List<Coin> froms = coinData.getFrom();
                byte[] fromSource;
                byte[] utxoFromSource;
                byte[] fromIndex;
                for (Coin from : froms) {
                    fromSource = from.getOwner();
                    utxoFromSource = new byte[tx.getHash().size()];
                    fromIndex = new byte[fromSource.length - utxoFromSource.length];
                    System.arraycopy(fromSource, 0, utxoFromSource, 0, tx.getHash().size());
                    System.arraycopy(fromSource, tx.getHash().size(), fromIndex, 0, fromIndex.length);

                    Transaction sourceTx = null;
                    try {
                        sourceTx = ledgerService.getTx(NulsDigestData.fromDigestHex(Hex.encode(fromSource)));
                    } catch (Exception e) {
                        continue;
                    }
                    if (sourceTx == null) {
                        return Result.getFailed(AccountLedgerErrorCode.SOURCE_TX_NOT_EXSITS);
                    }
                    byte[] address = sourceTx.getCoinData().getTo().get((int) new VarInt(fromIndex, 0).value).getOwner();
                    try {
                        fromMap.put(ArraysTool.joinintTogether(address, from.getOwner()), sourceTx.getCoinData().getTo().get((int) new VarInt(fromIndex, 0).value).serialize());
                    } catch (IOException e) {
                        throw new NulsRuntimeException(e);
                    }
                }
            }
            Result result = contractUtxoStorageService.batchSaveAndDeleteUTXO(fromMap, toList);
            if (result.isFailed() || result.getData() == null || (int) result.getData() != fromMap.size() + toList.size()) {
                return Result.getFailed();
            }
        }

        return Result.getSuccess();
    }

    @Override
    public Result<BigInteger> getBalance(byte[] address) {
        if (address == null || address.length != AddressTool.HASH_LENGTH) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }

        if (!ContractLedgerUtil.isContractAddress(address)) {
            return Result.getFailed(ContractErrorCode.CONTRACT_ADDRESS_NOT_EXIST);
        }

        //TODO pierre 查询合约余额
        BigInteger balance = null;

        if (balance == null) {
            return Result.getFailed(ContractErrorCode.CONTRACT_ADDRESS_NOT_EXIST);
        }

        return Result.getSuccess().setData(balance);
    }

}

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
import io.nuls.contract.ledger.manager.ContractBalanceManager;
import io.nuls.contract.ledger.module.ContractBalance;
import io.nuls.contract.ledger.service.ContractUtxoService;
import io.nuls.contract.ledger.util.ContractLedgerUtil;
import io.nuls.contract.storage.service.ContractTransferTransactionStorageService;
import io.nuls.contract.storage.service.ContractUtxoStorageService;
import io.nuls.core.tools.array.ArraysTool;
import io.nuls.core.tools.crypto.Hex;
import io.nuls.db.model.Entry;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.exception.NulsRuntimeException;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Component;
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
@Component
public class ContractUtxoServiceImpl implements ContractUtxoService {

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private ContractUtxoStorageService contractUtxoStorageService;

    @Autowired
    private ContractTransferTransactionStorageService contractTransferTransactionStorageService;

    @Autowired
    private ContractBalanceManager contractBalanceManager;

    @Autowired
    private AccountLedgerService accountLedgerService;


    /**
     * 从地址的维度上讲，分为两大类交易
     *   第一大类交易是普通地址转入合约地址
     *      合约交易 - 调用合约时 调用者地址向合约地址转入金额
     *      普通转账交易 - 普通地址向合约地址转入金额
     *   第二大类交易是智能合约特殊转账交易，合约地址转出到普通地址/合约地址
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
            // 在合约独立账本中，只有合约特殊转账交易才能从合约地址中转出金额，所以只有这类交易才处理fromCoinData -> delete - from
            List<byte[]> fromList = new ArrayList<>();
            // 合约特殊转账交易
            if(tx.getType() == ContractConstant.TX_TYPE_CONTRACT_TRANSFER) {
                List<Coin> froms = coinData.getFrom();
                byte[] fromSource;
                byte[] utxoFromTxHash;
                byte[] utxoFromIndex;
                int txHashSize = tx.getHash().size();
                Coin fromOfFromCoin;
                for (Coin from : froms) {
                    fromSource = from.getOwner();
                    utxoFromTxHash = new byte[txHashSize];
                    utxoFromIndex = new byte[fromSource.length - txHashSize];
                    System.arraycopy(fromSource, 0, utxoFromTxHash, 0, txHashSize);
                    System.arraycopy(fromSource, txHashSize, utxoFromIndex, 0, utxoFromIndex.length);

                    fromOfFromCoin = from.getFrom();

                    if (fromOfFromCoin == null) {
                        Transaction sourceTx = null;
                        try {
                            sourceTx = ledgerService.getTx(NulsDigestData.fromDigestHex(Hex.encode(utxoFromTxHash)));

                            /*if (sourceTx == null) {
                                sourceTx = contractTransferTransactionStorageService.getContractTransferTx(NulsDigestData.fromDigestHex(Hex.encode(utxoFromTxHash))).getData();
                            }*/
                        } catch (Exception e) {
                            throw new NulsRuntimeException(e);
                        }
                        if (sourceTx == null) {
                            return Result.getFailed(AccountLedgerErrorCode.SOURCE_TX_NOT_EXSITS);
                        }
                        fromOfFromCoin = sourceTx.getCoinData().getTo().get((int) new VarInt(utxoFromIndex, 0).value);
                    }

                    byte[] address = fromOfFromCoin.getOwner();

                    if (!ContractLedgerUtil.isContractAddress(address)) {
                        return Result.getFailed(ContractErrorCode.ILLEGAL_CONTRACT_ADDRESS);
                    }

                    fromList.add(fromSource);
                }
            }

            // save utxo - to
            List<Coin> tos = coinData.getTo();
            List<Entry<byte[], byte[]>> toList = new ArrayList<>();
            byte[] txHashBytes = null;
            txHashBytes = tx.getHash().getDigestBytes();
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
                    outKey = ArraysTool.concatenate(txHashBytes, new VarInt(i).encode());
                    toList.add(new Entry<byte[], byte[]>(outKey, to.serialize()));
                } catch (IOException e) {
                    throw new NulsRuntimeException(e);
                }
            }
            Result<List<Entry<byte[], byte[]>>> result = contractUtxoStorageService.batchSaveAndDeleteUTXO(toList, fromList);
            if (result.isFailed() || result.getData() == null) {
                return Result.getFailed();
            }
            // 刷新余额
            contractBalanceManager.refreshBalance(toList, result.getData());
        }
        return Result.getSuccess();
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
                outKey = ArraysTool.concatenate(txHashBytes, new VarInt(i).encode());
                toList.add(outKey);
            }

            // save - from
            List<Entry<byte[], byte[]>> fromList = new ArrayList<>();
            if(tx.getType() == ContractConstant.TX_TYPE_CONTRACT_TRANSFER) {
                List<Coin> froms = coinData.getFrom();
                int txHashSize = tx.getHash().size();
                byte[] fromSource;
                byte[] utxoFromHash;
                byte[] utxoFromIndex;
                Transaction sourceTx;
                Coin sourceTxCoinTo;
                byte[] address;
                for (Coin from : froms) {
                    fromSource = from.getOwner();
                    utxoFromHash = new byte[txHashSize];
                    utxoFromIndex = new byte[fromSource.length - txHashSize];
                    System.arraycopy(fromSource, 0, utxoFromHash, 0, txHashSize);
                    System.arraycopy(fromSource, txHashSize, utxoFromIndex, 0, utxoFromIndex.length);

                    try {
                        sourceTx = ledgerService.getTx(NulsDigestData.fromDigestHex(Hex.encode(utxoFromHash)));
                    } catch (Exception e) {
                        continue;
                    }
                    if (sourceTx == null) {
                        return Result.getFailed(AccountLedgerErrorCode.SOURCE_TX_NOT_EXSITS);
                    }

                    sourceTxCoinTo = sourceTx.getCoinData().getTo().get((int) new VarInt(utxoFromIndex, 0).value);
                    address = sourceTxCoinTo.getOwner();
                    try {
                        fromList.add(new Entry<byte[], byte[]>(fromSource, sourceTxCoinTo.serialize()));
                    } catch (IOException e) {
                        throw new NulsRuntimeException(e);
                    }
                }
            }
            /*
             * 回滚utxo, 保存原来的from，删除to
             * 回滚余额, 找到已删除的from 加回去， 筛选出已保存的to 减掉
             */
            List<Entry<byte[], byte[]>> deletedFromList = new ArrayList<>();
            for(Entry<byte[], byte[]> entry : fromList) {
                // 如果为空，则代表已被删除，回滚余额时需要把这个金额加回来
                if(contractUtxoStorageService.getUTXO(entry.getKey()) == null) {
                    deletedFromList.add(entry);
                }
            }

            // 函数将返回在数据库中存在的to
            Result<List<Entry<byte[], byte[]>>> result = contractUtxoStorageService.batchSaveAndDeleteUTXO(fromList, toList);
            if (result.isFailed() || result.getData() == null) {
                return Result.getFailed();
            }
            // 回滚余额, 找到已删除的from 加回去， 筛选出已保存的to 减掉
            contractBalanceManager.refreshBalance(deletedFromList, result.getData());

        }
        return Result.getSuccess();
    }

    @Override
    public Result<BigInteger> getBalance(byte[] address) {
        if (address == null || address.length != Address.ADDRESS_LENGTH) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }

        if (!ContractLedgerUtil.isContractAddress(address)) {
            return Result.getFailed(ContractErrorCode.CONTRACT_ADDRESS_NOT_EXIST);
        }

        ContractBalance contractBalance = contractBalanceManager.getBalance(address).getData();
        if(contractBalance == null) {
            return Result.getFailed(ContractErrorCode.DATA_ERROR);
        }
        BigInteger balance = BigInteger.valueOf(contractBalance.getBalance().getValue());

        return Result.getSuccess().setData(balance);
    }

}

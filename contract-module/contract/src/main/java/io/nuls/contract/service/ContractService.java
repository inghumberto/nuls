/**
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
 */
package io.nuls.contract.service;

import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.dto.ContractTransfer;
import io.nuls.contract.entity.tx.ContractTransferTransaction;
import io.nuls.contract.entity.txdata.CallContractData;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.entity.txdata.DeleteContractData;
import io.nuls.kernel.model.*;
import io.nuls.kernel.validate.ValidateResult;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/5/5
 */
public interface ContractService {

    /**
     * @param number 当前块编号
     * @param prevStateRoot 上一区块状态根
     * @param create 创建智能合约的参数
     * @return
     */
    //Result<ContractResult> createContract(long number, byte[] prevStateRoot, CreateContractData create);

    /**
     * @param number 当前块编号
     * @param prevStateRoot 上一区块状态根
     * @param call 调用智能合约的参数
     * @return
     */
    //Result<ContractResult> callContract(long number, byte[] prevStateRoot, CallContractData call);

    /**
     * @param number 当前块编号
     * @param prevStateRoot 上一区块状态根
     * @param delete 删除智能合约的参数
     * @return
     */
    //Result<ContractResult> deleteContract(long number, byte[] prevStateRoot, DeleteContractData delete);

    /**
     * @param address
     * @return
     */
    Result<Object> getContractInfo(String address);

    /**
     * @return
     */
    Result<Object> getVmStatus();

    boolean isContractAddress(byte[] addressBytes);

    /**
     * 保存 txInfo : key -> contractAddress + txHash, status is unconfirmed
     * 保存 UTXO : key -> contractAddress + txHash + index
     *
     * @param tx
     * @return
     */
    //Result<Integer> saveUnconfirmedTransaction(Transaction tx);


    /**
     * 合约转账交易
     *
     * @param from
     * @param to
     * @param values
     * @param blockTime
     * @param toMaps
     * @param contractUsedCoinMap
     * @return
     */
    /*Result transfer(byte[] from, byte[] to, Na values, long blockTime,
                                                Map<String, Coin> toMaps,
                                                Map<String, Coin> contractUsedCoinMap);*/

    /**
     * 保存 txInfo : key -> contractAddress + txHash, status is confirmed
     * 如果是非交易创建者则保存 UTXO : key -> contractAddress + txHash + index
     *
     * @param tx
     * @return
     */
    //Result<Integer> saveConfirmedTransaction(Transaction tx);
    Result<Integer> saveConfirmedTransactionList(List<Transaction> txs);


    /**
     *
     *
     * @param tx
     * @return
     */
    //Result<Integer> rollbackTransaction(Transaction tx);
    //Result<Integer> rollbackTransactionList(List<Transaction> txs);


    /**
     * 保存合约执行结果
     *
     * @param hash
     * @param result
     * @return
     */
    Result saveContractExecuteResult(NulsDigestData hash, ContractResult contractResult);

    /**
     * 删除合约执行结果
     *
     * @param hash
     * @return
     */
    Result deleteContractExecuteResult(NulsDigestData hash);

    /**
     * 根据地址检查是否存在这个合约执行结果
     *
     * @param hash
     * @return
     */
    boolean isExistContractExecuteResult(NulsDigestData hash);

    /**
     * 获取合约执行结果
     *
     * @param hash
     * @return
     */
    ContractResult getContractExecuteResult(NulsDigestData hash);

    /*****************************************************************************/

    /**
     * @param tx
     * @param height
     * @param stateRoot
     * @return
     */
    Result<ContractResult> callContract(Transaction tx, long height, byte[] stateRoot);

    /**
     * @param tx
     * @param contractResult
     */
    void rollbackContractTempBalance(Transaction tx, ContractResult contractResult);

    /**
     *
     */
    void removeContractTempBalance();

    /**
     * @param contractTransferTx
     * @param toMaps
     * @param fromSet
     * @return
     */
    ValidateResult verifyContractTransferCoinData(ContractTransferTransaction contractTransferTx, Map<String,Coin> toMaps, Set<String> fromSet);

    /**
     * @param transfer
     * @param blockTime
     * @param toMaps
     * @param contractUsedCoinMap
     * @return
     */
    Result<ContractTransferTransaction> createContractTransferTx(ContractTransfer transfer, long blockTime, Map<String, Coin> toMaps, Map<String, Coin> contractUsedCoinMap);

    /**
     * @param successContractTransferTxs
     * @param toMaps
     * @param fromSet
     * @param contractUsedCoinMap
     */
    void rollbackContractTransferTxs(Map<String, ContractTransferTransaction> successContractTransferTxs, Map<String, Coin> toMaps, Set<String> fromSet, Map<String, Coin> contractUsedCoinMap);

    /**
     * @param tx
     * @param toMaps
     * @param fromSet
     * @param contractUsedCoinMap
     */
    void rollbackContractTransferTx(ContractTransferTransaction tx, Map<String, Coin> toMaps, Set<String> fromSet, Map<String, Coin> contractUsedCoinMap);

    /**
     * @param sender
     * @param value
     * @param gasLimit
     * @param price
     * @param contractAddress
     * @param methodName
     * @param methodDesc
     * @param args
     * @param password
     * @param remark
     * @return
     */
    Result contractCallTx(byte[] sender, Na value, Long gasLimit, Long price, byte[] contractAddress,
                          String methodName, String methodDesc, String[] args, String password, String remark);

    /**
     * @param tx
     * @param toMaps
     * @param fromSet
     */
    void rollbackVerifyData(Transaction tx, Map<String,Coin> toMaps, Set<String> fromSet);

}

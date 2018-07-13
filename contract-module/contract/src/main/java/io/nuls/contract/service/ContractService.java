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

public interface ContractService {


    /**
     * 是否为合约地址
     *
     * @param addressBytes
     * @return
     */
    boolean isContractAddress(byte[] addressBytes);


    /**
     * 保存 txInfo : key -> contractAddress + txHash, status is confirmed
     * 保存 UTXO : key -> txHash + index
     *
     * @param txs
     * @return
     */
    Result<Integer> saveConfirmedTransactionList(List<Transaction> txs);


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


    /**
     * 执行合约
     *
     * @param tx
     * @param height
     * @param stateRoot
     * @return
     */
    Result<ContractResult> invokeContract(Transaction tx, long height, byte[] stateRoot);

    /**
     * 回滚一笔交易的合约临时余额区
     *
     * @param tx
     * @param contractResult
     */
    void rollbackContractTempBalance(Transaction tx, ContractResult contractResult);

    /**
     * 打包或者验证区块时，创建合约临时余额区
     *
     */
    void createContractTempBalance();

    /**
     * 移除合约临时余额区
     *
     */
    void removeContractTempBalance();

    /**
     * 验证合约内部转账的数据
     *
     * @param contractTransferTx
     * @param toMaps
     * @param fromSet
     * @return
     */
    ValidateResult verifyContractTransferCoinData(ContractTransferTransaction contractTransferTx, Map<String,Coin> toMaps, Set<String> fromSet);

    /**
     * 回滚验证数据
     *
     * @param tx
     * @param toMaps
     * @param fromSet
     */
    void rollbackVerifyData(Transaction tx, Map<String,Coin> toMaps, Set<String> fromSet);

    /**
     * 创建合约内部转账交易
     *
     * @param transfer
     * @param blockTime
     * @param toMaps
     * @param contractUsedCoinMap
     * @return
     */
    Result<ContractTransferTransaction> createContractTransferTx(ContractTransfer transfer, long blockTime, Map<String, Coin> toMaps, Map<String, Coin> contractUsedCoinMap);

    /**
     * 回滚合约内部转账交易列表
     *
     * @param successContractTransferTxs
     * @param toMaps
     * @param fromSet
     * @param contractUsedCoinMap
     */
    void rollbackContractTransferTxs(Map<String, ContractTransferTransaction> successContractTransferTxs, Map<String, Coin> toMaps, Set<String> fromSet, Map<String, Coin> contractUsedCoinMap);

    /**
     * 回滚合约内部转账交易
     *
     * @param tx
     * @param toMaps
     * @param fromSet
     * @param contractUsedCoinMap
     */
    void rollbackContractTransferTx(ContractTransferTransaction tx, Map<String, Coin> toMaps, Set<String> fromSet, Map<String, Coin> contractUsedCoinMap);

    /**
     * 创建调用合约的交易
     *
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

}

/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.contract.entity.tx.processor;

import io.nuls.contract.entity.tx.DeleteContractTransaction;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.entity.txdata.DeleteContractData;
import io.nuls.contract.ledger.manager.ContractBalanceManager;
import io.nuls.contract.storage.service.ContractAddressStorageService;
import io.nuls.contract.storage.service.ContractUtxoStorageService;
import io.nuls.db.model.Entry;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.model.Transaction;
import io.nuls.kernel.processor.TransactionProcessor;
import io.nuls.kernel.validate.ValidateResult;

import java.util.List;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/6/8
 */
public class DeleteContractTxProcessor implements TransactionProcessor<DeleteContractTransaction> {

    @Autowired
    private ContractAddressStorageService contractAddressStorageService;

    @Autowired
    private ContractUtxoStorageService contractUtxoStorageService;

    @Autowired
    private ContractBalanceManager contractBalanceManager;

    /**
     * 还原地址, 还原余额, 还原UTXO
     *
     * @param tx            要回滚的交易，The transaction to roll back.
     * @param secondaryData 辅助数据，视业务需要传递，Secondary data, depending on the business needs to be passed.
     * @return
     */
    @Override
    public Result onRollback(DeleteContractTransaction tx, Object secondaryData) {
        /*
        DeleteContractData txData = tx.getTxData();
        byte[] contractAddress = txData.getContractAddress();
        // 还原地址
        Result result = contractAddressStorageService.saveContractAddress(contractAddress);
        if(result.isSuccess()) {
            List<Entry<byte[], byte[]>> deleteList = tx.getDeleteList();
            // 还原UTXO
            result = contractUtxoStorageService.batchSaveAndDeleteUTXO(deleteList, null);
            if(result.isSuccess()) {
                // 还原余额
                contractBalanceManager.refreshBalance(deleteList, null);
            }
        }
        return result;
        */
        return null;
    }

    /**
     * 删除地址, 删除余额, 删除UTXO
     *
     * @param tx            要保存的交易，The transaction to save;
     * @param secondaryData 辅助数据，视业务需要传递，Secondary data, depending on the business needs to be passed.
     * @return
     */
    @Override
    public Result onCommit(DeleteContractTransaction tx, Object secondaryData) {
        /*
        Result<List<Entry<byte[], byte[]>>> listResult = null;
        DeleteContractData txData = tx.getTxData();
        byte[] contractAddress = txData.getContractAddress();
        // 删除地址
        Result result = contractAddressStorageService.deleteContractAddress(contractAddress);
        if(result.isSuccess()) {
            // 删除余额, 删除UTXO
            listResult = contractBalanceManager.removeBalance(contractAddress);
            tx.setDeleteList(listResult.getData());
        }
        return listResult;
        */
        return null;
    }

    @Override
    public ValidateResult conflictDetect(List<Transaction> txList) {
        //TODO pierre 检查冲突???
        return ValidateResult.getSuccessResult();
    }
}

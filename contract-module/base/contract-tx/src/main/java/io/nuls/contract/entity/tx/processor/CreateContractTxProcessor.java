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

import io.nuls.contract.entity.tx.CreateContractTransaction;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.contract.storage.service.ContractAddressStorageService;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Component;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.model.Transaction;
import io.nuls.kernel.processor.TransactionProcessor;
import io.nuls.kernel.validate.ValidateResult;

import java.util.List;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/6/7
 */
@Component
public class CreateContractTxProcessor implements TransactionProcessor<CreateContractTransaction> {

    @Autowired
    private ContractAddressStorageService contractAddressStorageService;

    @Override
    public Result onRollback(CreateContractTransaction tx, Object secondaryData) {
        /*
        CreateContractData txData = tx.getTxData();
        byte[] contractAddress = txData.getContractAddress();
        Result result = contractAddressStorageService.deleteContractAddress(contractAddress);
        return result;
        */
        return null;
    }

    @Override
    public Result onCommit(CreateContractTransaction tx, Object secondaryData) {
        /*
        CreateContractData txData = tx.getTxData();
        byte[] contractAddress = txData.getContractAddress();
        Result result = contractAddressStorageService.saveContractAddress(contractAddress);
        return result;
        */
        return null;
    }

    @Override
    public ValidateResult conflictDetect(List<Transaction> txList) {
        //TODO pierre 检查冲突???
        return ValidateResult.getSuccessResult();
    }
}

/**
 * MIT License
 * *
 * Copyright (c) 2017-2018 nuls.io
 * *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.protocol.model.validator;

import io.nuls.contract.constant.ContractConstant;
import io.nuls.kernel.constant.TransactionErrorCode;
import io.nuls.kernel.lite.annotation.Component;
import io.nuls.kernel.model.CoinData;
import io.nuls.kernel.model.Na;
import io.nuls.kernel.model.Transaction;
import io.nuls.kernel.utils.TransactionFeeCalculator;
import io.nuls.kernel.validate.NulsDataValidator;
import io.nuls.kernel.validate.ValidateResult;
import io.nuls.protocol.constant.ProtocolConstant;

/**
 * @author Niels
 */
@Component
public class TxFeeValidator implements NulsDataValidator<Transaction> {
    @Override
    public ValidateResult validate(Transaction tx) {
        int txType = tx.getType();
        if (tx.isSystemTx() || txType == ContractConstant.TX_TYPE_CONTRACT_TRANSFER) {
            return ValidateResult.getSuccessResult();
        }
        CoinData coinData = tx.getCoinData();
        if (null == coinData) {
            return ValidateResult.getFailedResult(this.getClass().getName(), TransactionErrorCode.FEE_NOT_RIGHT);
        }
        Na realFee = tx.getFee();
        Na fee = null;
        if(txType == ProtocolConstant.TX_TYPE_TRANSFER
                || txType == ContractConstant.TX_TYPE_CREATE_CONTRACT
                || txType == ContractConstant.TX_TYPE_CALL_CONTRACT
                || txType == ContractConstant.TX_TYPE_DELETE_CONTRACT){
            fee = TransactionFeeCalculator.getTransferFee(tx.size());
        }else{
            fee = TransactionFeeCalculator.getMaxFee(tx.size());
        }
        if (realFee.isGreaterOrEquals(fee)) {
            return ValidateResult.getSuccessResult();
        }
        return ValidateResult.getFailedResult(this.getClass().getName(), TransactionErrorCode.FEE_NOT_RIGHT);
    }
}

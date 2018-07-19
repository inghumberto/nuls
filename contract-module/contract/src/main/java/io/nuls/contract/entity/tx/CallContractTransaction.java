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
package io.nuls.contract.entity.tx;

import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.entity.txdata.CallContractData;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.model.Transaction;
import io.nuls.kernel.utils.NulsByteBuffer;

/**
 * @Desription:
 * @Author: PierreLuo
 * @Date: 2018/4/20
 */
public class CallContractTransaction extends Transaction<CallContractData> implements ContractTransaction{

    private ContractResult contractResult;

    public CallContractTransaction() {
        super(ContractConstant.TX_TYPE_CALL_CONTRACT);
    }

    @Override
    protected CallContractData parseTxData(NulsByteBuffer byteBuffer) throws NulsException {
        return byteBuffer.readNulsData(new CallContractData());
    }

    @Override
    public String getInfo(byte[] address) {
        //TODO auto-generated method stub
        return null;
    }

    @Override
    public ContractResult getContractResult() {
        return contractResult;
    }

    @Override
    public void setContractResult(ContractResult contractResult) {
        this.contractResult = contractResult;
    }


}

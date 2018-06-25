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
package io.nuls.contract.entity.tx;

import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.dto.ContractTransfer;
import io.nuls.contract.entity.txdata.CreateContractData;
import io.nuls.kernel.constant.NulsConstant;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.model.Transaction;
import io.nuls.kernel.model.TransactionLogicData;
import io.nuls.kernel.utils.NulsByteBuffer;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/6/7
 */
public class ContractTransferTransaction extends Transaction {

    private ContractTransfer transfer;

    public ContractTransferTransaction() {
        super(ContractConstant.TX_TYPE_CONTRACT_TRANSFER);
    }

    @Override
    protected CreateContractData parseTxData(NulsByteBuffer byteBuffer) throws NulsException {
        byteBuffer.readBytes(NulsConstant.PLACE_HOLDER.length);
        return null;
    }

    @Override
    public String getInfo(byte[] address) {
        //TODO pierre
        return null;
    }

    @Override
    public boolean needVerifySignature() {
        return false;
    }

    public ContractTransfer getTransfer() {
        return transfer;
    }

    public ContractTransferTransaction setTransfer(ContractTransfer transfer) {
        this.transfer = transfer;
        return this;
    }
}

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
package io.nuls.contract.entity.txdata;


import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.model.TransactionLogicData;
import io.nuls.kernel.utils.NulsByteBuffer;
import io.nuls.kernel.utils.NulsOutputStreamBuffer;

import java.io.IOException;
import java.util.Set;

/**
 * @Author: PierreLuo
 */
public class ContractTransferData extends TransactionLogicData{

    //TODO pierre 增加更多成员变量，丰富数据完整性，带来的弊端是增加了网络流量

    private byte success;

    public ContractTransferData(){

    }

    public ContractTransferData(byte success) {
        this.success = success;
    }

    @Override
    public int size() {
        int size = 0;
        size += 1;
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(success);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.success = byteBuffer.readByte();
    }

    @Override
    public Set<byte[]> getAddresses() {
        return null;
    }

    public byte getSuccess() {
        return success;
    }

    public void setSuccess(byte success) {
        this.success = success;
    }
}

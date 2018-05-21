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
import io.nuls.kernel.utils.SerializeUtils;

import java.io.IOException;
import java.util.Set;

/**
 * @Desription:
 * @Author: PierreLuo
 * @Date: 2018/4/21
 */
public class CallContractData extends TransactionLogicData {

    private byte[] address;
    private byte[] contractAddress;
    private byte[] naLimit;
    private byte price;
    private byte argsCount;
    private Object[] args;

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBytes(address);
        size += SerializeUtils.sizeOfBytes(contractAddress);
        size += SerializeUtils.sizeOfBytes(naLimit);
        size += 1;
        size += 1;
        if(args != null) {
            for(Object arg : args) {
                if(arg instanceof Integer) {
                    size += SerializeUtils.sizeOfVarInt((Integer) arg);
                } else if(arg instanceof Long) {
                    size += SerializeUtils.sizeOfVarInt((Long) arg);
                }
            }
        }
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(address);
        stream.writeBytesWithLength(contractAddress);
        stream.writeBytesWithLength(naLimit);
        stream.write(price);
        stream.write(argsCount);
        if(args != null) {
            for(Object arg : args) {
                if(arg instanceof Integer) {
                    stream.writeVarInt((Integer) arg);
                } else if(arg instanceof Long) {
                    stream.writeVarInt((Long) arg);
                }
            }
        }
    }

    @Override
    protected void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.address = byteBuffer.readByLengthByte();
        this.contractAddress = byteBuffer.readByLengthByte();
        this.naLimit = byteBuffer.readByLengthByte();
        this.price = byteBuffer.readByte();
        this.argsCount = byteBuffer.readByte();
        int length = this.argsCount;
        this.args = new Object[length];
        for(int i = 0; i < length; i++) {
            args[i] = byteBuffer.readVarInt();
        }
    }

    public byte[] getAddress() {
        return address;
    }

    public void setAddress(byte[] address) {
        this.address = address;
    }

    public byte[] getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(byte[] contractAddress) {
        this.contractAddress = contractAddress;
    }

    public byte[] getNaLimit() {
        return naLimit;
    }

    public void setNaLimit(byte[] naLimit) {
        this.naLimit = naLimit;
    }

    public byte getPrice() {
        return price;
    }

    public void setPrice(byte price) {
        this.price = price;
    }

    public byte getArgsCount() {
        return argsCount;
    }

    public void setArgsCount(byte argsCount) {
        this.argsCount = argsCount;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    @Override
    public Set<byte[]> getAddresses() {
        //TODO auto-generated method stub
        return null;
    }
}

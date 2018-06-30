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

    private byte[] sender;
    private byte[] contractAddress;
    private long value;
    private long gasLimit;
    private long price;
    private String methodName;
    private String methodDesc;
    private long txGasUsed;
    private byte argsCount;
    private String[] args;

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfBytes(sender);
        size += SerializeUtils.sizeOfBytes(contractAddress);
        size += SerializeUtils.sizeOfVarInt(value);
        size += SerializeUtils.sizeOfVarInt(gasLimit);
        size += SerializeUtils.sizeOfVarInt(price);
        size += SerializeUtils.sizeOfString(methodName);
        size += SerializeUtils.sizeOfString(methodDesc);
        size += SerializeUtils.sizeOfVarInt(txGasUsed);
        size += 1;
        if(args != null) {
            for(String arg : args) {
                size += SerializeUtils.sizeOfString(arg);
            }
        }
        return size;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(sender);
        stream.writeBytesWithLength(contractAddress);
        stream.writeVarInt(value);
        stream.writeVarInt(gasLimit);
        stream.writeVarInt(price);
        stream.writeString(methodName);
        stream.writeString(methodDesc);
        stream.writeVarInt(txGasUsed);
        stream.write(argsCount);
        if(args != null) {
            for(String arg : args) {
                stream.writeString(arg);
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.sender = byteBuffer.readByLengthByte();
        this.contractAddress = byteBuffer.readByLengthByte();
        this.value = byteBuffer.readVarInt();
        this.gasLimit = byteBuffer.readVarInt();
        this.price = byteBuffer.readVarInt();
        this.methodName = byteBuffer.readString();
        this.methodDesc = byteBuffer.readString();
        this.txGasUsed = byteBuffer.readVarInt();
        this.argsCount = byteBuffer.readByte();
        int length = this.argsCount;
        this.args = new String[length];
        for(int i = 0; i < length; i++) {
            args[i] = byteBuffer.readString();
        }
    }

    public byte[] getSender() {
        return sender;
    }

    public void setSender(byte[] sender) {
        this.sender = sender;
    }

    public byte[] getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(byte[] contractAddress) {
        this.contractAddress = contractAddress;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(long gasLimit) {
        this.gasLimit = gasLimit;
    }

    public long getPrice() {
        return price;
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public long getTxGasUsed() {
        return txGasUsed;
    }

    public void setTxGasUsed(long txGasUsed) {
        this.txGasUsed = txGasUsed;
    }

    public byte getArgsCount() {
        return argsCount;
    }

    public void setArgsCount(byte argsCount) {
        this.argsCount = argsCount;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public void args(String... args) {
        this.args = args;
    }

    @Override
    public Set<byte[]> getAddresses() {
        return null;
    }
}

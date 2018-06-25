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
package io.nuls.contract.dto;

import io.nuls.core.tools.crypto.Base58;
import io.nuls.kernel.model.Na;
import io.nuls.kernel.model.NulsDigestData;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/6/16
 */
public class ContractTransfer {

    private byte[] from;

    private byte[] to;

    private BigInteger value;

    private NulsDigestData hash;

    public byte[] getFrom() {
        return from;
    }

    public void setFrom(byte[] from) {
        this.from = from;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public NulsDigestData getHash() {
        return hash;
    }

    public void setHash(NulsDigestData hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "ContractTransfer{" +
                "from=" + Arrays.toString(from) +
                ", to=" + Arrays.toString(to) +
                ", value=" + value +
                ", hash=" + hash +
                '}';
    }
}

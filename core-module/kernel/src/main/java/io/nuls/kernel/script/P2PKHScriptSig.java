/*
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
 *
 */
package io.nuls.kernel.script;

import io.nuls.core.tools.crypto.ECKey;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.model.NulsDigestData;
import io.nuls.kernel.model.NulsSignData;
import io.nuls.kernel.utils.NulsByteBuffer;
import io.nuls.kernel.utils.NulsOutputStreamBuffer;
import io.nuls.kernel.utils.SerializeUtils;
import io.nuls.kernel.validate.ValidateResult;

import java.io.IOException;

/**
 * @author Facjas
 */
public class P2PKHScriptSig extends Script {

    public static final int DEFAULT_SERIALIZE_LENGTH = 110;

    private NulsSignData signData;

    private byte[] publicKey;

    public P2PKHScriptSig() {

    }

    public P2PKHScriptSig(byte[] signBytes, byte[] publicKey) {
        this.signData = new NulsSignData();
        try {
            this.signData.parse(signBytes,0);
        } catch (NulsException e) {
            Log.error(e);
        }
        this.publicKey = publicKey;
    }

    public NulsSignData getSignData() {
        return signData;
    }

    public void setSignData(NulsSignData signData) {
        this.signData = signData;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public P2PKHScriptSig(NulsSignData signData, byte[] publicKey) {
        this.signData = signData;
        this.publicKey = publicKey;
    }

    public ValidateResult verifySign(NulsDigestData digestData) {
        boolean b = ECKey.verify(digestData.getDigestBytes(), signData.getSignBytes(), this.getPublicKey());
        if (b) {
            return ValidateResult.getSuccessResult();
        } else {
            return ValidateResult.getFailedResult(this.getClass().getName(), KernelErrorCode.SIGNATURE_ERROR);
        }
    }

    public byte[] getSignerHash160() {
        return SerializeUtils.sha256hash160(getPublicKey());
    }

    public static P2PKHScriptSig createFromBytes(byte[] bytes) throws NulsException {
        P2PKHScriptSig sig = new P2PKHScriptSig();
        sig.parse(bytes,0);
        return sig;
    }

    @Override
    public byte[] getBytes() {
        try {
            return this.serialize();
        } catch (IOException e) {
            Log.error(e);
            return null;
        }
    }

    /**
     * serialize important field
     */
    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.write(publicKey.length);
        stream.write(publicKey);
        stream.writeNulsData(signData);

    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        int length = byteBuffer.readByte();
        this.publicKey = byteBuffer.readBytes(length);
        this.signData = new NulsSignData();
        this.signData.parse(byteBuffer);

    }

    @Override
    public int size() {
        int size = 1 + publicKey.length;
        size += SerializeUtils.sizeOfNulsData(signData);
        return size;
    }
}

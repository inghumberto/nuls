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
package io.nuls.protocol.model.validator;

import io.nuls.contract.constant.ContractConstant;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.lite.annotation.Component;
import io.nuls.kernel.model.Block;
import io.nuls.kernel.model.Transaction;
import io.nuls.kernel.validate.NulsDataValidator;
import io.nuls.kernel.validate.ValidateResult;
import io.nuls.protocol.constant.ProtocolConstant;

/**
 * @author Niels
 */
@Component
public class BlockMaxSizeValidator implements NulsDataValidator<Block> {

    @Override
    public ValidateResult validate(Block data) {
        if (data == null) {
            return ValidateResult.getFailedResult(this.getClass().getName(), KernelErrorCode.DATA_NOT_FOUND);
        }
        long length = 0L;
        for (Transaction tx : data.getTxs()) {
            // pierre add 验证区块大小 - 合约内部转账交易不能跳过
            if (tx.isSystemTx() && tx.getType() != ContractConstant.TX_TYPE_CONTRACT_TRANSFER) {
                continue;
            }
            length += tx.size();
        }
        if (length > ProtocolConstant.MAX_BLOCK_SIZE) {
            return ValidateResult.getFailedResult(this.getClass().getName(), KernelErrorCode.BLOCK_TOO_BIG);
        }
        return ValidateResult.getSuccessResult();
    }
}

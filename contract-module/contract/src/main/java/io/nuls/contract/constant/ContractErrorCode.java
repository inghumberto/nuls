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
package io.nuls.contract.constant;

import io.nuls.kernel.constant.ErrorCode;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.constant.TransactionErrorCode;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/6/17
 */
public interface ContractErrorCode extends TransactionErrorCode, KernelErrorCode {

    ErrorCode CONTRACT_EXECUTE_ERROR = ErrorCode.init("CONTRACT001", "100001");

    ErrorCode CONTRACT_ADDRESS_NOT_EXIST = ErrorCode.init("CONTRACT002", "100002");

    ErrorCode CONTRACT_TX_CREATE_ERROR = ErrorCode.init("CONTRACT003", "100003");

    ErrorCode ILLEGAL_CONTRACT_ADDRESS = ErrorCode.init("CONTRACT004", "100004");

    ErrorCode NON_CONTRACTUAL_TRANSACTION = ErrorCode.init("CONTRACT005", "100005");

}

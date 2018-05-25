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
package io.nuls.contract.storage.service;

import io.nuls.account.model.Account;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.model.Transaction;

import java.util.List;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/5/24
 */
public interface ContractStorageService {
    /**
     * 保存本地合约账户
     *
     * @param account
     * @return
     */
    Result saveContractAddress(Account account);

    /**
     * 根据合约地址删除本地合约账户
     *
     * @param contractAddressBytes
     * @return
     */
    Result<Account> deleteContractAddress(byte[] contractAddressBytes);

    /**
     * 根据合约地址获取本地合约账户详细信息
     *
     * @param contractAddressBytes
     * @return
     */
    Result<Account> getContractAddress(byte[] contractAddressBytes);

    /**
     * 获取本地所有创建的合约账户详细信息
     *
     * @return
     */
    Result<List<Account>> getContractAddressList();
}

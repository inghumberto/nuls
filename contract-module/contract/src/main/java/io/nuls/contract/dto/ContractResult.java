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

package io.nuls.contract.dto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/4/20
 */
public class ContractResult {

    /**
     * 合约地址
     */
    private byte[] contractAddress;

    /**
     * 合约执行结果
     */
    private String result;
    /**
     * 已使用Gas
     */
    private long gasUsed;
    /**
     * 状态根
     */
    private byte[] stateRoot;

    private boolean error;

    private String errorMessage;

    private String stackTrace;

    private BigInteger balance;

    private BigInteger nonce;

    /**
     * 合约转账交易
     */
    private List<ContractTransfer> transfers = new ArrayList<>();

    /**
     * 消息事件
     */
    private List<String> events = new ArrayList<>();


    public byte[] getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(byte[] contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public void setStateRoot(byte[] stateRoot) {
        this.stateRoot = stateRoot;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public void setGasUsed(long gasUsed) {
        this.gasUsed = gasUsed;
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events;
    }

    public List<ContractTransfer> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<ContractTransfer> transfers) {
        this.transfers = transfers;
    }
}

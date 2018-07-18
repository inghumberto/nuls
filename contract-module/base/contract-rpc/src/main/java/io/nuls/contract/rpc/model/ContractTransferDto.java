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
package io.nuls.contract.rpc.model;

import io.nuls.contract.dto.ContractTransfer;
import io.nuls.kernel.model.NulsDigestData;
import io.nuls.kernel.utils.AddressTool;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.math.BigInteger;

/**
 * @author: PierreLuo
 */
@ApiModel(value = "ContractTransferDtoJSON")
public class ContractTransferDto {

    @ApiModelProperty(name = "from", value = "付款方")
    private String from;
    @ApiModelProperty(name = "to", value = "收款方")
    private String to;
    @ApiModelProperty(name = "value", value = "转账金额")
    private BigInteger value;
    @ApiModelProperty(name = "txHash", value = "交易hash")
    private String txHash;

    public ContractTransferDto(ContractTransfer transfer) {
        this.from = AddressTool.getStringAddressByBytes(transfer.getFrom());
        this.to = AddressTool.getStringAddressByBytes(transfer.getTo());
        this.value = transfer.getValue();
        this.txHash = transfer.getHash();
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public BigInteger getValue() {
        return value;
    }

    public void setValue(BigInteger value) {
        this.value = value;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}

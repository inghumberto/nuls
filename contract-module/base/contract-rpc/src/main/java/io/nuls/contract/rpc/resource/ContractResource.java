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
package io.nuls.contract.rpc.resource;

import io.nuls.account.ledger.constant.AccountLedgerErrorCode;
import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.contract.rpc.form.ContractCall;
import io.nuls.contract.rpc.form.ContractCreate;
import io.nuls.contract.rpc.form.ContractDelete;
import io.nuls.contract.service.ContractTxService;
import io.nuls.core.tools.str.StringUtils;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Component;
import io.nuls.kernel.model.Na;
import io.nuls.kernel.model.Result;
import io.nuls.kernel.utils.AddressTool;
import io.swagger.annotations.*;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Base64;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/5/23
 */
@Path("/contranct")
@Api(value = "/contranct", description = "contranct")
@Component
public class ContractResource {

    @Autowired
    private ContractTxService contractTxService;

    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "创建智能合约")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public Result createContract(@ApiParam(name = "createForm", value = "创建智能合约", required = true) ContractCreate create) {
        if (create == null || create.getValue() < 0 || create.getGasLimit() < 0 || create.getPrice() < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }

        if (!AddressTool.validAddress(create.getSender())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR);
        }

        String contractCode = create.getContractCode();
        if(StringUtils.isBlank(contractCode)) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }

        byte[] contractCodeBytes = Base64.getDecoder().decode(contractCode);

        return contractTxService.contractCreateTx(create.getSender(),
                Na.valueOf(create.getValue()),
                create.getGasLimit(),
                create.getPrice(),
                contractCodeBytes,
                create.getArgs(),
                create.getPassword(),
                create.getRemark());
    }

    @POST
    @Path("/call")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "调用智能合约")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public Result callContract(@ApiParam(name = "callFrom", value = "调用智能合约", required = true) ContractCall call) {
        if (call == null || call.getValue() < 0 || call.getGasLimit() < 0 || call.getPrice() < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }

        if (!AddressTool.validAddress(call.getSender())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR);
        }

        if (!AddressTool.validAddress(call.getContractAddress())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR);
        }

        return contractTxService.contractCallTx(call.getSender(),
                Na.valueOf(call.getValue()),
                call.getGasLimit(),
                call.getPrice(),
                call.getContractAddress(),
                call.getMethodName(),
                call.getMethodDesc(),
                call.getArgs(),
                call.getPassword(),
                call.getRemark());
    }

    @POST
    @Path("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "删除智能合约")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public Result deleteContract(@ApiParam(name = "deleteFrom", value = "删除智能合约", required = true) ContractDelete delete) {
        if (delete == null) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR);
        }

        if (!AddressTool.validAddress(delete.getSender())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR);
        }

        if (!AddressTool.validAddress(delete.getContractAddress())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR);
        }

        return contractTxService.contractDeleteTx(delete.getSender(),
                delete.getContractAddress(),
                delete.getPassword(),
                delete.getRemark());
    }



}

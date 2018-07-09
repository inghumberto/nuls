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
import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.helper.VMHelper;
import io.nuls.contract.rpc.form.ContractCall;
import io.nuls.contract.rpc.form.ContractCreate;
import io.nuls.contract.rpc.form.ContractDelete;
import io.nuls.contract.rpc.model.ContractAccountUtxoDto;
import io.nuls.contract.rpc.model.ContractResultDto;
import io.nuls.contract.rpc.model.ContractUtxoDto;
import io.nuls.contract.service.ContractService;
import io.nuls.contract.service.ContractTxService;
import io.nuls.contract.storage.service.ContractAddressStorageService;
import io.nuls.contract.storage.service.ContractUtxoStorageService;
import io.nuls.contract.util.ContractCoinComparator;
import io.nuls.contract.vm.program.ProgramExecutor;
import io.nuls.contract.vm.program.ProgramMethod;
import io.nuls.core.tools.crypto.Hex;
import io.nuls.core.tools.log.Log;
import io.nuls.core.tools.str.StringUtils;
import io.nuls.db.model.Entry;
import io.nuls.kernel.context.NulsContext;
import io.nuls.kernel.exception.NulsException;
import io.nuls.kernel.lite.annotation.Autowired;
import io.nuls.kernel.lite.annotation.Component;
import io.nuls.kernel.lite.core.bean.InitializingBean;
import io.nuls.kernel.model.*;
import io.nuls.kernel.utils.AddressTool;
import io.nuls.ledger.constant.LedgerErrorCode;
import io.swagger.annotations.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.*;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/5/23
 */
@Path("/contranct")
@Api(value = "/contranct", description = "contranct")
@Component
public class ContractResource implements InitializingBean {

    @Autowired
    private ContractTxService contractTxService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private ContractAddressStorageService contractAddressStorageService;

    @Autowired
    private ContractUtxoStorageService contractUtxoStorageService;

    @Autowired
    private VMHelper vmHelper;

    private ProgramExecutor programExecutor;

    @Override
    public void afterPropertiesSet() throws NulsException {
        programExecutor = vmHelper.getProgramExecutor();
    }

    @POST
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "创建智能合约")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public RpcClientResult createContract(@ApiParam(name = "createForm", value = "创建智能合约", required = true) ContractCreate create) {
        if (create == null || create.getGasLimit() < 0 || create.getPrice() < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR).toRpcClientResult();
        }

        if (!AddressTool.validAddress(create.getSender())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR).toRpcClientResult();
        }

        String contractCode = create.getContractCode();
        if(StringUtils.isBlank(contractCode)) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER).toRpcClientResult();
        }

        byte[] contractCodeBytes = Hex.decode(contractCode);

        return contractTxService.contractCreateTx(create.getSender(),
                create.getGasLimit(),
                create.getPrice(),
                contractCodeBytes,
                create.getArgs(),
                create.getPassword(),
                create.getRemark()).toRpcClientResult();
    }

    @POST
    @Path("/call")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "调用智能合约")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public RpcClientResult callContract(@ApiParam(name = "callFrom", value = "调用智能合约", required = true) ContractCall call) {
        if (call == null || call.getValue() < 0 || call.getGasLimit() < 0 || call.getPrice() < 0) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR).toRpcClientResult();
        }

        if (!AddressTool.validAddress(call.getSender())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR).toRpcClientResult();
        }

        if (!AddressTool.validAddress(call.getContractAddress())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR).toRpcClientResult();
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
                call.getRemark()).toRpcClientResult();
    }

    @POST
    @Path("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "删除智能合约")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public RpcClientResult deleteContract(@ApiParam(name = "deleteFrom", value = "删除智能合约", required = true) ContractDelete delete) {
        if (delete == null) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR).toRpcClientResult();
        }

        if (!AddressTool.validAddress(delete.getSender())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR).toRpcClientResult();
        }

        if (!AddressTool.validAddress(delete.getContractAddress())) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR).toRpcClientResult();
        }

        return contractTxService.contractDeleteTx(delete.getSender(),
                delete.getContractAddress(),
                delete.getPassword(),
                delete.getRemark()).toRpcClientResult();
    }

    @GET
    @Path("/{address}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "验证是否为合约地址")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public RpcClientResult validateContractAddress(@ApiParam(name="address", value="地址", required = true)
                                                   @PathParam("address") String address) {
        if (StringUtils.isBlank(address)) {
            return Result.getFailed(LedgerErrorCode.NULL_PARAMETER).toRpcClientResult();
        }
        if (!AddressTool.validAddress(address)) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR).toRpcClientResult();
        }

        try {
            boolean isContractAddress = contractAddressStorageService.isExistContractAddress(AddressTool.getAddress(address));
            return Result.getSuccess().setData(isContractAddress).toRpcClientResult();
        } catch (Exception e) {
            return Result.getFailed().setData(e.getMessage()).toRpcClientResult();
        }
    }

    @GET
    @Path("/info/{address}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取智能合约信息")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public RpcClientResult getContractInfo(@ApiParam(name = "address", value = "合约地址", required = true) @PathParam("address") String contractAddress) {
        if (contractAddress == null) {
            return Result.getFailed(ContractErrorCode.PARAMETER_ERROR).toRpcClientResult();
        }

        if (!AddressTool.validAddress(contractAddress)) {
            return Result.getFailed(AccountLedgerErrorCode.ADDRESS_ERROR).toRpcClientResult();
        }

        byte[] prevStateRoot = NulsContext.getInstance().getBestStateRoot();
        byte[] contractAddressBytes = AddressTool.getAddress(contractAddress);

        ProgramExecutor track = programExecutor.begin(prevStateRoot);
        List<ProgramMethod> methods = track.method(contractAddressBytes);

        for (ProgramMethod method : methods) {
            Log.info(method.toString());
        }

        return Result.getSuccess().setData(methods).toRpcClientResult();
    }


    @GET
    @Path("/tx/{hash}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "获取智能合约执行结果")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success", response = ContractResultDto.class)
    })
    public RpcClientResult getContractTxResult(@ApiParam(name="hash", value="交易hash", required = true)
                                          @PathParam("hash") String hash) {
        if (StringUtils.isBlank(hash)) {
            return Result.getFailed(LedgerErrorCode.NULL_PARAMETER).toRpcClientResult();
        }
        if (!NulsDigestData.validHash(hash)) {
            return Result.getFailed(LedgerErrorCode.PARAMETER_ERROR).toRpcClientResult();
        }

        ContractResultDto contractResultDto = new ContractResultDto();
        ContractResult contractExecuteResult = null;
        try {
            contractExecuteResult = contractService.getContractExecuteResult(NulsDigestData.fromDigestHex(hash));
            if(contractExecuteResult != null) {
                contractResultDto.setContractAddress(AddressTool.getStringAddressByBytes(contractExecuteResult.getContractAddress()));
                contractResultDto.setResult(contractExecuteResult.getResult());
                contractResultDto.setGasUsed(contractExecuteResult.getGasUsed());
                contractResultDto.setStateRoot(Hex.encode(contractExecuteResult.getStateRoot()));
                contractResultDto.setValue(contractExecuteResult.getValue());
                contractResultDto.setError(contractExecuteResult.isError());
                contractResultDto.setErrorMessage(contractExecuteResult.getErrorMessage());
                contractResultDto.setStackTrace(contractExecuteResult.getStackTrace());
                contractResultDto.setBalance(contractExecuteResult.getBalance());
                contractResultDto.setNonce(contractExecuteResult.getNonce());
                contractResultDto.setTransfers(contractExecuteResult.getTransfers());
                contractResultDto.setEvents(contractExecuteResult.getEvents());
            }
            return Result.getSuccess().setData(contractResultDto).toRpcClientResult();
        } catch (NulsException e) {
            return Result.getFailed().setData(e.getMessage()).toRpcClientResult();
        }
    }


    @GET
    @Path("/limit/{address}/{limit}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "根据address和limit查询合约UTXO")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success", response = ContractAccountUtxoDto.class)
    })
    public RpcClientResult getUtxoByAddressAndLimit(
            @ApiParam(name="address", value="地址", required = true) @PathParam("address") String address,
            @ApiParam(name="limit", value="数量", required = true) @PathParam("limit") Integer limit) {
        if (StringUtils.isBlank(address) || limit == null) {
            return Result.getFailed(LedgerErrorCode.NULL_PARAMETER).toRpcClientResult();
        }
        if (!AddressTool.validAddress(address)) {
            return Result.getFailed(LedgerErrorCode.PARAMETER_ERROR).toRpcClientResult();
        }
        Result result = null;
        try {
            List<Coin> coinList = getAllUtxoByAddress(address);
            int limitValue = limit.intValue();
            ContractAccountUtxoDto accountUtxoDto = new ContractAccountUtxoDto();
            List<ContractUtxoDto> list = new LinkedList<>();
            int i = 0;
            for (Coin coin : coinList) {
                //TODO pierre 测试时注释
                /*if (!coin.usable()) {
                    continue;
                }*/
                if (coin.getNa().equals(Na.ZERO)) {
                    continue;
                }
                list.add(new ContractUtxoDto(coin));
                i++;
                if(i >= limitValue) {
                    break;
                }
            }
            accountUtxoDto.setUtxoDtoList(list);
            result = Result.getSuccess().setData(accountUtxoDto);
            return result.toRpcClientResult();
        } catch (Exception e) {
            Log.error(e);
            result = Result.getFailed(LedgerErrorCode.SYS_UNKOWN_EXCEPTION);
            return result.toRpcClientResult();
        }
    }

    @GET
    @Path("/amount/{address}/{amount}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "根据address和amount查询合约UTXO")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success", response = ContractAccountUtxoDto.class)
    })
    public RpcClientResult getUtxoByAddressAndAmount(
            @ApiParam(name="address", value="地址", required = true) @PathParam("address") String address,
            @ApiParam(name="amount", value="金额", required = true) @PathParam("amount") Long amount) {
        if (StringUtils.isBlank(address) || amount == null) {
            return Result.getFailed(LedgerErrorCode.NULL_PARAMETER).toRpcClientResult();
        }
        if (!AddressTool.validAddress(address)) {
            return Result.getFailed(LedgerErrorCode.PARAMETER_ERROR).toRpcClientResult();
        }
        Result result = null;
        try {
            List<Coin> coinList = getAllUtxoByAddress(address);
            Na amountNa = Na.valueOf(amount.longValue());

            ContractAccountUtxoDto accountUtxoDto = new ContractAccountUtxoDto();
            List<ContractUtxoDto> list = new LinkedList<>();
            Na values = Na.ZERO;
            for (Coin coin : coinList) {
                //TODO pierre 测试时注释
                /*if (!coin.usable()) {
                    continue;
                }*/
                if (coin.getNa().equals(Na.ZERO)) {
                    continue;
                }
                list.add(new ContractUtxoDto(coin));
                values = values.add(coin.getNa());

                if (values.isGreaterOrEquals(amountNa)) {
                    break;
                }
            }
            accountUtxoDto.setUtxoDtoList(list);
            result = Result.getSuccess().setData(accountUtxoDto);
            return result.toRpcClientResult();
        } catch (Exception e) {
            Log.error(e);
            result = Result.getFailed(LedgerErrorCode.SYS_UNKOWN_EXCEPTION);
            return result.toRpcClientResult();
        }
    }

    private List<Coin> getAllUtxoByAddress(String address) {
        List<Coin> coinList = new ArrayList<>();
        byte[] addressBytes = AddressTool.getAddress(address);
        List<Entry<byte[], byte[]>> coinBytesList = contractUtxoStorageService.loadAllCoinList();
        Coin coin;
        for (Entry<byte[], byte[]> coinEntryBytes : coinBytesList) {
            coin = new Coin();
            try {
                coin.parse(coinEntryBytes.getValue(), 0);
            } catch (NulsException e) {
                Log.info("parse coin form db error");
                continue;
            }
            if (Arrays.equals(coin.getOwner(), addressBytes)) {
                coin.setOwner(coinEntryBytes.getKey());
                coinList.add(coin);
            }
        }
        Collections.sort(coinList, ContractCoinComparator.getInstance());
        return coinList;
    }

}

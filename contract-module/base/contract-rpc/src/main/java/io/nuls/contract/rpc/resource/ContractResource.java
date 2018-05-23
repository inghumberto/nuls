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

import io.nuls.contract.constant.ContractErrorCode;
import io.nuls.core.tools.log.Log;
import io.nuls.core.tools.str.StringUtils;
import io.nuls.kernel.exception.NulsRuntimeException;
import io.nuls.kernel.lite.annotation.Component;
import io.nuls.kernel.model.Result;
import io.swagger.annotations.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @desription:
 * @author: PierreLuo
 * @date: 2018/5/23
 */
@Path("/contranct")
@Api(value = "/contranct", description = "contranct")
@Component
public class ContractResource {


    @GET
    @Path("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "根据hash查询交易")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "success")
    })
    public Result createContract(@ApiParam(name="hash", value="交易hash", required = true)
                          @PathParam("hash") String hash) {
        if (StringUtils.isBlank(hash)) {
            return Result.getFailed(ContractErrorCode.NULL_PARAMETER);
        }
        Result result = null;
        try {

        } catch (NulsRuntimeException re) {
            Log.error(re);
            result = new Result(false, re.getCode(), re.getMessage());
        } catch (Exception e) {
            Log.error(e);
            result = Result.getFailed(ContractErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        return result;
    }



}

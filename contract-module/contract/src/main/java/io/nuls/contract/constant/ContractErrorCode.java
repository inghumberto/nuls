package io.nuls.contract.constant;

import io.nuls.kernel.constant.ErrorCode;
import io.nuls.kernel.constant.KernelErrorCode;

public interface ContractErrorCode extends KernelErrorCode {

    //TODO pierre 合约执行错误
    ErrorCode CONTRACT_EXECUTE_ERROR = ErrorCode.init("CONTRACT001", 100001);

}

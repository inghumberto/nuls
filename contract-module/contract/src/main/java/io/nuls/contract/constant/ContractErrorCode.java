package io.nuls.contract.constant;

import io.nuls.kernel.constant.ErrorCode;
import io.nuls.kernel.constant.KernelErrorCode;
import io.nuls.kernel.constant.TransactionErrorCode;

public interface ContractErrorCode extends TransactionErrorCode, KernelErrorCode {

    ErrorCode CONTRACT_EXECUTE_ERROR = ErrorCode.init("CONTRACT001", "100001");

    //TODO pierre 错误码
    ErrorCode CONTRACT_ADDRESS_NOT_EXIST = null;

}

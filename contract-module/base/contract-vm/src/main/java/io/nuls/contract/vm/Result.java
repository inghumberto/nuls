package io.nuls.contract.vm;

import io.nuls.contract.vm.code.VariableType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Result {

    private VariableType variableType;

    private Object value;

    private boolean ended;

    private boolean exception;

    private boolean error;

    public Result() {
    }

    public Result(VariableType variableType) {
        this.variableType = variableType;
    }

    public void value(Object value) {
        this.value = value;
        this.ended = true;
    }

    public void exception(ObjectRef exception) {
        this.value(exception);
        this.variableType = exception.getVariableType();
        this.exception = true;
    }

    public void error(ObjectRef error) {
        this.value(error);
        this.variableType = error.getVariableType();
        this.error = true;
    }

}

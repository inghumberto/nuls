package io.nuls.contract.vm.program;

import java.util.List;

public class ProgramMethod {

    private String name;

    private String desc;

    private List<String> args;

    private String returnArg;

    public ProgramMethod() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public String getReturnArg() {
        return returnArg;
    }

    public void setReturnArg(String returnArg) {
        this.returnArg = returnArg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgramMethod that = (ProgramMethod) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (desc != null ? !desc.equals(that.desc) : that.desc != null) return false;
        if (args != null ? !args.equals(that.args) : that.args != null) return false;
        return returnArg != null ? returnArg.equals(that.returnArg) : that.returnArg == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (desc != null ? desc.hashCode() : 0);
        result = 31 * result + (args != null ? args.hashCode() : 0);
        result = 31 * result + (returnArg != null ? returnArg.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ProgramMethod{" +
                "name='" + name + '\'' +
                ", desc='" + desc + '\'' +
                ", args=" + args +
                ", returnArg='" + returnArg + '\'' +
                '}';
    }

}

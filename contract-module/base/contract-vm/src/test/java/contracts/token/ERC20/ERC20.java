package contracts.token.ERC20;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;

public interface ERC20 extends ERC20Basic {

    BigInteger allowance(Address owner, Address spender);

    boolean transferFrom(Address from, Address to, BigInteger value);

    boolean approve(Address spender, BigInteger value);

    @Data
    @AllArgsConstructor
    class ApprovalEvent implements Event {

        private Address owner;

        private Address spender;

        private BigInteger value;

    }

}

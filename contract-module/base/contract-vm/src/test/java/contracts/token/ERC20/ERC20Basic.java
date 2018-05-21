package contracts.token.ERC20;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;

public interface ERC20Basic {

    BigInteger totalSupply();

    BigInteger balanceOf(Address who);

    boolean transfer(Address to, BigInteger value);

    @Data
    @AllArgsConstructor
    class TransferEvent implements Event {

        private Address from;

        private Address to;

        private BigInteger value;

    }

}

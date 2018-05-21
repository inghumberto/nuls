package contracts.token.ERC20;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Event;
import io.nuls.contract.sdk.Msg;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;

import static io.nuls.contract.sdk.Utils.emit;

public class BurnableToken extends BasicToken {

    public void burn(BigInteger value) {
        Address burner = Msg.sender();
        subtractBalance(burner, value);
        totalSupply = totalSupply.subtract(value);
        emit(new BurnEvent(burner, value));
        emit(new TransferEvent(burner, null, value));
    }

    @Data
    @AllArgsConstructor
    class BurnEvent implements Event {

        private Address burner;

        private BigInteger value;

    }

}

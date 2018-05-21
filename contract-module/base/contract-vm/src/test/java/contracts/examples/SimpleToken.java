package contracts.examples;

import contracts.token.ERC20.StandardToken;
import io.nuls.contract.sdk.Msg;

import java.math.BigInteger;

import static io.nuls.contract.sdk.Utils.emit;

public class SimpleToken extends StandardToken {

    private final String name = "SimpleToken";
    private final String symbol = "SIM";
    private final int decimals = 18;
    private final BigInteger initialSupply = new BigInteger("10000").multiply(BigInteger.TEN.pow(decimals));

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getDecimals() {
        return decimals;
    }

    public BigInteger getInitialSupply() {
        return initialSupply;
    }

    public SimpleToken() {
        totalSupply = initialSupply;
        balances.put(Msg.sender(), initialSupply);
        emit(new TransferEvent(null, Msg.sender(), initialSupply));
    }

}

package contracts.examples;

import contracts.token.ERC20.MintableToken;
import io.nuls.contract.sdk.Contract;
import io.nuls.contract.sdk.Msg;

import java.math.BigInteger;

import static io.nuls.contract.sdk.Utils.emit;

public class TestToken extends MintableToken implements Contract {

    private final String name = "TestToken";
    private final String symbol = "TT";
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

    public TestToken() {
        totalSupply = initialSupply;
        balances.put(Msg.sender(), initialSupply);
        emit(new TransferEvent(null, Msg.sender(), initialSupply));
    }

}

package contracts.examples;

import contracts.crowdsale.Crowdsale;
import io.nuls.contract.sdk.Address;

import java.math.BigInteger;

public class TestCrowdsale extends Crowdsale {

    public TestCrowdsale(BigInteger rate, Address wallet, Address token) {
        super(rate, wallet, token);
    }

}

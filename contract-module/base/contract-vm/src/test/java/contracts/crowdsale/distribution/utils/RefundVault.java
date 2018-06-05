package contracts.crowdsale.distribution.utils;

import contracts.ownership.OwnableImpl;
import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Event;
import io.nuls.contract.sdk.Msg;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;

public class RefundVault extends OwnableImpl {

    public int Active = 1;
    public int Refunding = 2;
    public int Closed = 3;

    private Map<Address, BigInteger> deposited = new HashMap<>();
    private Address wallet;
    private int state;

    public Map<Address, BigInteger> getDeposited() {
        return deposited;
    }

    public Address getWallet() {
        return wallet;
    }

    public int getState() {
        return state;
    }

    class Closed implements Event {

    }

    class RefundsEnabled implements Event {

    }

    class Refunded implements Event {

        private Address beneficiary;
        private BigInteger weiAmount;

        public Refunded(Address beneficiary, BigInteger weiAmount) {
            this.beneficiary = beneficiary;
            this.weiAmount = weiAmount;
        }

        @Override
        public String toString() {
            return "Refunded{" +
                    "beneficiary=" + beneficiary +
                    ", weiAmount=" + weiAmount +
                    '}';
        }

    }

    public RefundVault(Address wallet) {
        super();
        this.wallet = wallet;
        this.state = Active;
    }

    public void deposit(Address investor) {
        onlyOwner();
        require(state == Active);
        deposited.put(investor, deposited.getOrDefault(investor, BigInteger.ZERO).add(Msg.value()));
    }

    public void close() {
        onlyOwner();
        require(state == Active);
        state = Closed;
        emit(new Closed());
        wallet.transfer(Msg.address().balance());
    }

    public void enableRefunds() {
        onlyOwner();
        require(state == Active);
        state = Refunding;
        emit(new RefundsEnabled());
    }

    public void refund(Address investor) {
        require(state == Refunding);
        BigInteger depositedValue = deposited.getOrDefault(investor, BigInteger.ZERO);
        deposited.put(investor, BigInteger.ZERO);
        investor.transfer(depositedValue);
        emit(new Refunded(investor, depositedValue));
    }

}

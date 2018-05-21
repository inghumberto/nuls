package contracts.token.ERC20;

import contracts.ownership.Ownable;
import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Event;
import io.nuls.contract.sdk.Msg;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigInteger;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;

public class MintableToken extends StandardToken implements Ownable {

    private boolean mintingFinished = false;

    private Address owner;

    public MintableToken() {
        this.owner = Msg.sender();
    }

    public boolean isMintingFinished() {
        return mintingFinished;
    }

    @Override
    public Address getOwner() {
        return owner;
    }

    @Override
    public void onlyOwner() {
        require(Msg.sender().equals(owner));
    }

    @Override
    public void transferOwnership(Address newOwner) {
        onlyOwner();
        emit(new OwnershipTransferredEvent(owner, newOwner));
        owner = newOwner;
    }

    @Override
    public void renounceOwnership() {
        onlyOwner();
        emit(new OwnershipRenouncedEvent(owner));
        owner = null;
    }

    public void canMint() {
        require(!mintingFinished);
    }

    public void hasMintPermission() {
        require(Msg.sender().equals(owner));
    }

    public boolean mint(Address to, BigInteger amount) {
        hasMintPermission();
        canMint();
        check(amount);
        totalSupply = totalSupply.add(amount);
        addBalance(to, amount);
        emit(new MintEvent(to, amount));
        emit(new TransferEvent(null, to, amount));
        return true;
    }

    public boolean finishMinting() {
        onlyOwner();
        canMint();
        mintingFinished = true;
        emit(new MintFinishedEvent());
        return true;
    }

    @Data
    @AllArgsConstructor
    class MintEvent implements Event {

        private Address to;

        private BigInteger amount;

    }

    @Data
    @AllArgsConstructor
    class MintFinishedEvent implements Event {

    }

}

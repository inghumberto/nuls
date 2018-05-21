package contracts.ownership;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Msg;

import static io.nuls.contract.sdk.Utils.emit;
import static io.nuls.contract.sdk.Utils.require;

public class OwnableImpl implements Ownable {

    private Address owner;

    public OwnableImpl() {
        this.owner = Msg.sender();
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

}

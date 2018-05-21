package contracts.ownership;

import io.nuls.contract.sdk.Address;
import io.nuls.contract.sdk.Event;
import lombok.AllArgsConstructor;
import lombok.Data;

public interface Ownable {

    Address getOwner();

    void onlyOwner();

    void transferOwnership(Address newOwner);

    void renounceOwnership();

    @Data
    @AllArgsConstructor
    class OwnershipRenouncedEvent implements Event {

        private Address previousOwner;

    }

    @Data
    @AllArgsConstructor
    class OwnershipTransferredEvent implements Event {

        private Address previousOwner;

        private Address newOwner;

    }

}

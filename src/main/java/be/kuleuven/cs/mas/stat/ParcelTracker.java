package be.kuleuven.cs.mas.stat;

import be.kuleuven.cs.mas.parcel.ParcelObserver;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;

import java.util.HashSet;
import java.util.Set;

public abstract class ParcelTracker implements ParcelObserver {

    public static final int DELIVERY_TRESHOLD = 100;

    private Set<TimeAwareParcel> parcels = new HashSet<>();
    private int delivered = 0;

    public void track(TimeAwareParcel parcel) {
        parcels.add(parcel);
        parcel.registerObserver(this);
    }

    @Override
    public void parcelDelivered(TimeAwareParcel parcel) {
        delivered++;

        if (delivered >= DELIVERY_TRESHOLD) {
            deliveryTresholdReached();
        }
    }

    public abstract void deliveryTresholdReached();

}

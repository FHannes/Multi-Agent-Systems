package be.kuleuven.cs.mas.stat;

import be.kuleuven.cs.mas.parcel.ParcelObserver;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class ParcelTracker implements ParcelObserver {

    private int treshold;

    private Set<TimeAwareParcel> parcels = new HashSet<>();
    private int delivered = 0;

    public ParcelTracker(int treshold) {
        this.treshold = treshold;
    }

    public void track(TimeAwareParcel parcel) {
        parcels.add(parcel);
        parcel.registerObserver(this);
    }

    @Override
    public void parcelDelivered(TimeAwareParcel parcel) {
        delivered++;

        if (delivered >= treshold) {
            deliveryTresholdReached();
        }
    }

    public Iterator<TimeAwareParcel> iterator() {
        return parcels.iterator();
    }

    public abstract void deliveryTresholdReached();

}

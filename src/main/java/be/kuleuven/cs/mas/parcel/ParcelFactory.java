package be.kuleuven.cs.mas.parcel;

import be.kuleuven.cs.mas.strategy.FieldStrategy;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

/**
 * A factory which can be sued to create instances of {@link TimeAwareParcel}.
 */
public class ParcelFactory {

    public final static double MAGNITUDE = 1.0D;

    private final RandomGenerator rng;
    private final FieldStrategy fieldStrategy;
    private final List<Point> storageSites;
    private final List<Point> ioSites;

    public ParcelFactory(RandomGenerator rng, FieldStrategy fieldStrategy, List<Point> storageSites,
                         List<Point> ioSites) {
        this.rng = rng;
        this.fieldStrategy = fieldStrategy;
        this.storageSites = new ArrayList<>(storageSites);
        this.ioSites = new ArrayList<>(ioSites);
    }

    /**
     * Creates a new {@link TimeAwareParcel} to use in a {@link com.github.rinde.rinsim.core.model.pdp.PDPModel}.
     *
     * @param outgoing
     *        The parcel generated has to be outgoing. This means that it is to be picked up from one of the shelves in
     *        the warehouse and transported to a drop-off site where the warehouse's I/O occurs.
     * @param currentTime
     * @return A new {@link TimeAwareParcel} instance.
     */
    public TimeAwareParcel makeParcel(boolean outgoing, long currentTime) {
        // Get set of shelf sites and drop-off sites
        List<Point> sources;
        List<Point> targets;
        if (outgoing) {
            sources = storageSites;
            targets = ioSites;
        } else {
            sources = ioSites;
            targets = storageSites;
        }

        // Return null if all source sites are occupied by waiting parcels
        if (sources.isEmpty()) {
            return null;
        }

        // Select random source and destination point
        Point source = sources.get(rng.nextInt(sources.size()));
        Point target = targets.get(rng.nextInt(targets.size()));

        // Create and return emitter
        return new TimeAwareParcel(fieldStrategy, source, target, MAGNITUDE, currentTime);
    }

    public TimeAwareParcel makeParcel(long currentTime) {
        boolean outgoing = rng.nextBoolean();
        TimeAwareParcel parcel = makeParcel(outgoing, currentTime);
        if (parcel == null) {
            parcel = makeParcel(!outgoing, currentTime);
        }
        return parcel;
    }

}

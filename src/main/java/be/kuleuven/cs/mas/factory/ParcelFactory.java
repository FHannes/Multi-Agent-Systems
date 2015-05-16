package be.kuleuven.cs.mas.factory;

import be.kuleuven.cs.mas.GraphUtils;
import be.kuleuven.cs.mas.gradientfield.FieldEmitter;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;
import com.github.rinde.rinsim.geom.Point;

import java.util.*;
import java.util.stream.Collectors;

public class ParcelFactory {

    private Random random = new Random();

    /**
     * Creates a parcel based on the current state of the given {@link GradientModel}, with as purpose that it be added
     * to that model. It takes into account other parcels that may have already been added to the field.
     *
     * @param model
     * @param pMagnitude
     * @param currentTime
     * @param outgoing
     *        The parcel generated has to be outgoing. This means that it is to be picked up from one of the shelves in
     *        the warehouse and transported to a drop-off site where the warehouse's I/O occurs.
     * @return A {@link TimeAwareParcel} which is added to the given {@link GradientModel}. The method can return null
     *         if all available source sites are already occupied by waiting parcels.
     */
    public TimeAwareParcel makeParcel(GradientModel model, double pMagnitude, long currentTime, boolean outgoing) {
        // Get set of shelf sites and drop-off sites
        List<Point> sources;
        List<Point> targets;
        if (outgoing) {
            sources = GraphUtils.getShelfSites();
            targets = GraphUtils.getDropOffSites();
        } else {
            sources = GraphUtils.getDropOffSites();
            targets = GraphUtils.getShelfSites();
        }

        // Filter out all source points where a parcel can be waiting that does not have a waiting parcel already
        // Process with HashSet for performance
        Set<Point> sourceSet = new HashSet<>(sources);
        List<FieldEmitter> waitingParcels = model.getEmitters().stream().filter(e -> e instanceof TimeAwareParcel &&
                e.getPosition().isPresent()).collect(Collectors.toList());
        waitingParcels.stream().forEach(f -> {
            if (sourceSet.contains(f.getPosition().get())) {
                sourceSet.remove(f.getPosition().get());
            }
        });
        sources = new ArrayList<>(sourceSet);

        // Return null if all source sites are occupied by waiting parcels
        if (sources.isEmpty()) {
            return null;
        }

        // Select random source and destination point
        Point source = sources.get(random.nextInt(sources.size()));
        Point target = targets.get(random.nextInt(sources.size()));

        // Create and return emitter
        return new TimeAwareParcel(source, target, pMagnitude, currentTime);
    }

    public TimeAwareParcel makeParcel(GradientModel model, double pMagnitude, long currentTime) {
        boolean outgoing = random.nextBoolean();
        TimeAwareParcel parcel = makeParcel(model, pMagnitude, currentTime, outgoing);
        if (parcel == null) {
            parcel = makeParcel(model, pMagnitude, currentTime, !outgoing);
        }
        return parcel;
    }

}

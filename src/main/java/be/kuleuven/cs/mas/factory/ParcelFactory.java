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
     * Creates a parcel to be positioned on a given {@link GradientModel}.
     *
     * @param model
     * @param pMagnitude
     * @param currentTime
     * @return
     */
    public TimeAwareParcel makeParcel(GradientModel model, double pMagnitude, long currentTime) {
        // Get set of shelf sites and drop-off sites
        List<Point> sources;
        List<Point> targets;
        if (random.nextBoolean()) {
            sources = GraphUtils.getDropOffSites();
            targets = GraphUtils.getShelfSites();
        } else {
            sources = GraphUtils.getShelfSites();
            targets = GraphUtils.getDropOffSites();
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

        // Select random source and destination point
        Point source = sources.get(random.nextInt(sources.size()));
        Point target = targets.get(random.nextInt(sources.size()));

        // Create and return emitter
        return new TimeAwareParcel(source, target, pMagnitude, currentTime);
    }

}

package be.kuleuven.cs.mas.agent;

import be.kuleuven.cs.mas.strategy.FieldStrategy;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AgentFactory implements RandomUser {

    public final static double CAPACITY = 1.0D;

    private RandomGenerator rng;
    private final FieldStrategy fieldStrategy;
    private final RoadModel roadModel;
    private final int visualRange;
    private final List<Point> spawnSites;
    private int idCounter = 0;

    public AgentFactory(FieldStrategy fieldStrategy, RoadModel roadModel, int visualRange, List<Point> spawnSites) {
        this.fieldStrategy = fieldStrategy;
        this.roadModel = roadModel;
        this.visualRange = visualRange;
        this.spawnSites = spawnSites;
    }

    public AGVAgent makeAgent() {
        Set<Point> occupiedPoints = roadModel.getObjectsOfType(AGVAgent.class).stream().map(AGVAgent::getPosition)
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());

        List<Point> freeSpawns = spawnSites.stream().filter(p -> !occupiedPoints.contains(p)).collect(Collectors.toList());

        if (freeSpawns.isEmpty()) {
            return null;
        } else {
            return new AGVAgent(rng, fieldStrategy, visualRange, freeSpawns.get(rng.nextInt(freeSpawns.size())),
                    String.format("agent%d", ++idCounter), CAPACITY);
        }
    }

    @Override
    public void setRandomGenerator(RandomProvider provider) {
        rng = provider.sharedInstance(AgentFactory.class);
    }

}

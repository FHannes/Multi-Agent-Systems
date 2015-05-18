package be.kuleuven.cs.mas.agent;

import be.kuleuven.cs.mas.AGVAgent;
import be.kuleuven.cs.mas.strategy.FieldStrategy;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AgentFactory {

    public final static int VISUAL_RANGE = 3;

    private final RandomGenerator rng;
    private final FieldStrategy fieldStrategy;
    private final List<Point> spawnSites;
    private int idCounter = 0;

    public AgentFactory(RandomGenerator rng, FieldStrategy fieldStrategy, List<Point> spawnSites) {
        this.rng = rng;
        this.fieldStrategy = fieldStrategy;
        this.spawnSites = spawnSites;
    }

    public AGVAgent makeAgent(RoadModel roadModel) {
        Set<Point> occupiedPoints = roadModel.getObjectsOfType(AGVAgent.class).stream().map(AGVAgent::getPosition)
                .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());

        List<Point> freeSpawns = spawnSites.stream().filter(p -> !occupiedPoints.contains(p)).collect(Collectors.toList());

        if (freeSpawns.isEmpty()) {
            return null;
        } else {
            return new AGVAgent(rng, fieldStrategy, VISUAL_RANGE, freeSpawns.get(rng.nextInt(freeSpawns.size())),
                    String.format("agent%d", ++idCounter));
        }
    }

}

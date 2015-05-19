package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.agent.AGVAgent;
import be.kuleuven.cs.mas.agent.AgentFactory;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import be.kuleuven.cs.mas.parcel.ParcelFactory;
import be.kuleuven.cs.mas.strategy.FieldStrategy;
import be.kuleuven.cs.mas.strategy.FieldTimeStrategy;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.pdptw.common.PDPRoadModel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.Random;

public class Main {

    private RandomGenerator rng = RandomGeneratorFactory.createRandomGenerator(new Random());

    private FieldStrategy agentFieldStrategy = new FieldTimeStrategy(1000);
    private AgentFactory agentFactory = new AgentFactory(rng, agentFieldStrategy, GraphUtils.VISUAL_RANGE,
            GraphUtils.getSpawnSites());

    private FieldStrategy parcelFieldStrategy = new FieldTimeStrategy(1000);
    private ParcelFactory parcelFactory = new ParcelFactory(rng, parcelFieldStrategy, GraphUtils.getShelfSites(),
            GraphUtils.getDropOffSites());

    public static void main(String[] args) {
        final Simulator sim = Simulator.builder()
                .addModel(new PDPRoadModel(CollisionGraphRoadModel.builder(GraphUtils.createGraph())
                        .setVehicleLength(GraphUtils.VEHICLE_LENGTH)
                        .build(), true))
                .addModel(new GradientModel())
                .build();

        /*for (int i = 0; i < 20; i++) {
            sim.register(new AGVAgent(sim.getRandomGenerator(), GraphUtils.VISUAL_RANGE));
        }

        View.create(sim)
                .with(GraphRoadModelRenderer.builder()
                                .setMargin((int) GraphUtils.VEHICLE_LENGTH)
                                .showNodes()
                                .showDirectionArrows()
                )
                .with(AGVRenderer.builder()
                                .useDifferentColorsForVehicles()
                )
                .show();*/
    }

}

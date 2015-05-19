package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.agent.AgentFactory;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import be.kuleuven.cs.mas.parcel.ParcelFactory;
import be.kuleuven.cs.mas.strategy.FieldStrategy;
import be.kuleuven.cs.mas.strategy.FieldTimeStrategy;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.Random;

public class Main {

    private final RandomGenerator rng = RandomGeneratorFactory.createRandomGenerator(new Random());

    private final FieldStrategy agentFieldStrategy = new FieldTimeStrategy(1000);
    private final AgentFactory agentFactory;

    private final FieldStrategy parcelFieldStrategy = new FieldTimeStrategy(1000);
    private final ParcelFactory parcelFactory;

    private final RoadModel roadModel;

    private final Simulator sim;

    public Main() {
        roadModel = CollisionGraphRoadModel.builder(GraphUtils.createGraph())
                .setVehicleLength(GraphUtils.VEHICLE_LENGTH)
                .build();

        agentFactory = new AgentFactory(rng, agentFieldStrategy, roadModel, GraphUtils.VISUAL_RANGE,
                GraphUtils.getSpawnSites());
        parcelFactory = new ParcelFactory(rng, parcelFieldStrategy, GraphUtils.getShelfSites(),
                GraphUtils.getDropOffSites());

        sim = Simulator.builder()
                .addModel(DefaultPDPModel.create())
                .addModel(roadModel)
                .addModel(new GradientModel())
                .build();
    }

    public void populate() {
        //sim.register(agentFactory.makeAgent());
    }

    public void run() {
        View.create(sim)
                .with(GraphRoadModelRenderer.builder()
                                .setMargin((int) GraphUtils.VEHICLE_LENGTH)
                                .showNodes()
                                .showDirectionArrows()
                )
                .with(AGVRenderer.builder()
                                .useDifferentColorsForVehicles()
                )
                .show();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.populate();
        main.run();
    }

}

package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.agent.AgentFactory;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import be.kuleuven.cs.mas.parcel.ParcelFactory;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;
import be.kuleuven.cs.mas.strategy.FieldStrategy;
import be.kuleuven.cs.mas.strategy.FieldTimeStrategy;
import be.kuleuven.cs.mas.strategy.FieldTresholdStrategy;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.Random;

public class Main {

    private final RandomGenerator rng = RandomGeneratorFactory.createRandomGenerator(new Random());

    private final FieldStrategy agentFieldStrategy = new FieldTresholdStrategy(60000, 1D, 5D);
    private final AgentFactory agentFactory;

    private final FieldStrategy parcelFieldStrategy = new FieldTresholdStrategy(60000, 1D, 5D);
    private final ParcelFactory parcelFactory;

    private final CommModel commModel;
    private final PDPModel pdpModel;
    private final RoadModel roadModel;

    private final Simulator sim;

    public Main() {
        commModel = CommModel.builder().build();
        pdpModel = DefaultPDPModel.create();
        roadModel = CollisionGraphRoadModel.builder(GraphUtils.createGraph())
                .setVehicleLength(GraphUtils.VEHICLE_LENGTH)
                .build();

        agentFactory = new AgentFactory(rng, agentFieldStrategy, roadModel, GraphUtils.VISUAL_RANGE,
                GraphUtils.getSpawnSites());
        parcelFactory = new ParcelFactory(rng, parcelFieldStrategy, GraphUtils.getShelfSites(),
                GraphUtils.getDropOffSites());

        sim = Simulator.builder()
                .addModel(commModel)
                .addModel(pdpModel)
                .addModel(roadModel)
                .addModel(new GradientModel())
                .build();
    }

    public void populate() {
        sim.register(agentFactory.makeAgent());
        sim.register(agentFactory.makeAgent());
        sim.register(agentFactory.makeAgent());

        sim.register(parcelFactory.makeParcel(sim.getCurrentTime()));
        sim.register(parcelFactory.makeParcel(sim.getCurrentTime()));
        sim.register(parcelFactory.makeParcel(sim.getCurrentTime()));
        sim.register(parcelFactory.makeParcel(sim.getCurrentTime()));
        sim.register(parcelFactory.makeParcel(sim.getCurrentTime()));
    }

    public void run() {
        View.create(sim)
                .with(GraphRoadModelRenderer.builder()
                                .setMargin((int) GraphUtils.VEHICLE_LENGTH)
                                .showNodes()
                                .showDirectionArrows()
                )
                .with(RoadUserRenderer.builder()
                                .addImageAssociation(TimeAwareParcel.class, "/graphics/perspective/deliverypackage3.png"))
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

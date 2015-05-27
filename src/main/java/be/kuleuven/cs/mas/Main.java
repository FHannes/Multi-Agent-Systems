package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.agent.AgentFactory;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import be.kuleuven.cs.mas.parcel.ParcelFactory;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;
import be.kuleuven.cs.mas.render.GradientGraphRoadModelRenderer;
import be.kuleuven.cs.mas.strategy.FieldStrategy;
import be.kuleuven.cs.mas.strategy.FieldTresholdStrategy;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.rand.RandomUser;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;

public class Main implements RandomUser {

    public static final int AGENTS = 20;
    public static final int PARCELS = 25;

    private RandomGenerator rng;

    private final FieldStrategy agentFieldStrategy = new FieldTresholdStrategy(60000, 0.25D, 1.25D);
    private final AgentFactory agentFactory;

    private final FieldStrategy parcelFieldStrategy = new FieldTresholdStrategy(60000, 1D, 5D);
    private final ParcelFactory parcelFactory;

    private final RandomModel rndModel;
    private final CommModel commModel;
    private final PDPModel pdpModel;
    private final RoadModel roadModel;

    private final Simulator sim;

    public Main() {
        rndModel = RandomModel.create(123);
        commModel = CommModel.builder().build();
        pdpModel = DefaultPDPModel.create();
        roadModel = CollisionGraphRoadModel.builder(GraphUtils.createGraph())
                .setVehicleLength(GraphUtils.VEHICLE_LENGTH)
                .build();

        rndModel.register(this);

        agentFactory = new AgentFactory(rng, agentFieldStrategy, roadModel, GraphUtils.VISUAL_RANGE,
                GraphUtils.getSpawnSites());
        parcelFactory = new ParcelFactory(rng, parcelFieldStrategy, GraphUtils.getShelfSites(),
                GraphUtils.getDropOffSites());

        sim = Simulator.builder()
                .addModel(rndModel)
                .addModel(commModel)
                .addModel(pdpModel)
                .addModel(roadModel)
                .addModel(new GradientModel())
                .build();
    }

    public void populate() {
        for (int i = 0; i < AGENTS; i++) {
            sim.register(agentFactory.makeAgent());
        }

        for (int i = 0; i < PARCELS; i++) {
            sim.register(parcelFactory.makeParcel(sim.getCurrentTime()));
        }
    }

    public void run() {
        View.create(sim)
                .with(GradientGraphRoadModelRenderer.builder()
                                .setMargin((int) GraphUtils.VEHICLE_LENGTH)
                                .showNodes()
                                .showNodeCoordinates()
                                .showDirectionArrows()
                )
                .with(RoadUserRenderer.builder()
                        .addImageAssociation(TimeAwareParcel.class, "/graphics/perspective/deliverypackage3.png"))
                .with(AGVRenderer.builder()
                                .useDifferentColorsForVehicles()
                )
                .setResolution(1000, 1000)
                .show();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.populate();
        main.run();
    }

    @Override
    public void setRandomGenerator(RandomProvider provider) {
        rng = provider.masterInstance();
    }

}

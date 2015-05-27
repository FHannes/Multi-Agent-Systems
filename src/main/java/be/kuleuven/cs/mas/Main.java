package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.agent.AgentFactory;
import be.kuleuven.cs.mas.gradientfield.GFConfig;
import be.kuleuven.cs.mas.parcel.ParcelFactory;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;
import be.kuleuven.cs.mas.render.GradientGraphRoadModelRenderer;
import be.kuleuven.cs.mas.scenario.GFObjFunc;
import be.kuleuven.cs.mas.scenario.GradientScenario;
import be.kuleuven.cs.mas.strategy.FieldStrategy;
import be.kuleuven.cs.mas.strategy.FieldTresholdStrategy;
import com.github.rinde.rinsim.experiment.Experiment;
import com.github.rinde.rinsim.experiment.MASConfiguration;
import com.github.rinde.rinsim.pdptw.common.ObjectiveFunction;
import com.github.rinde.rinsim.scenario.Scenario;
import com.github.rinde.rinsim.scenario.ScenarioController;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.Random;

public class Main {

    private final FieldStrategy agentFieldStrategy = new FieldTresholdStrategy(60000, 0.25D, 1.25D);
    private final AgentFactory agentFactory;

    private final FieldStrategy parcelFieldStrategy = new FieldTresholdStrategy(60000, 1D, 5D);
    private final ParcelFactory parcelFactory;

    private final ScenarioController.UICreator uic;

    private final MASConfiguration config;
    private final Scenario scenario;
    private final ObjectiveFunction objFunc;

    public Main() {
        uic = (sim) -> View.create(sim)
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
                .setResolution(1000, 1000).show();

        agentFactory = new AgentFactory(agentFieldStrategy, GraphUtils.VISUAL_RANGE,
                GraphUtils.getSpawnSites());
        parcelFactory = new ParcelFactory( parcelFieldStrategy, GraphUtils.getShelfSites(),
                GraphUtils.getDropOffSites());

        config = new GFConfig(agentFactory, parcelFactory);
        scenario = new GradientScenario();
        objFunc = new GFObjFunc();
    }

    public void run() {
        Experiment.build(objFunc)
                .withRandomSeed(123) // TODO: Use same seed for all rng
                .withThreads(1)
                .addConfiguration(config)
                .addScenario(scenario)
                .showGui(uic)
                .repeat(1)
                .perform();
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.run();
    }

}

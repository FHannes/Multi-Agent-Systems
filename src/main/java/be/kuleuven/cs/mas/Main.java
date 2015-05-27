package be.kuleuven.cs.mas;

import be.kuleuven.cs.mas.agent.AgentFactory;
import be.kuleuven.cs.mas.gradientfield.GradientModel;
import be.kuleuven.cs.mas.parcel.ParcelFactory;
import be.kuleuven.cs.mas.parcel.ParcelScheduler;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;
import be.kuleuven.cs.mas.render.GradientGraphRoadModelRenderer;
import be.kuleuven.cs.mas.stat.ParcelTracker;
import be.kuleuven.cs.mas.strategy.FieldStrategy;
import be.kuleuven.cs.mas.strategy.FieldTresholdStrategy;
import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.*;
import com.github.rinde.rinsim.core.model.rand.RandomModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public class Main {

    public static final int AGENTS = 20;

    private final String fileOut;

    private final FieldStrategy agentFieldStrategy = new FieldTresholdStrategy(60000, 0.25D, 1.25D);
    private final AgentFactory agentFactory;

    private final FieldStrategy parcelFieldStrategy = new FieldTresholdStrategy(60000, 1D, 5D);
    private final ParcelFactory parcelFactory;

    private ParcelScheduler parcelScheduler;
    private ParcelTracker parcelTracker;

    private final RandomModel rndModel;
    private final CommModel commModel;
    private final PDPModel pdpModel;
    private final RoadModel roadModel;

    private final Simulator sim;

    public Main(long seed, int treshold, String fileOut) {
        this.fileOut = fileOut;

        rndModel = RandomModel.create(seed);
        commModel = CommModel.builder().build();
        pdpModel = DefaultPDPModel.create();
        roadModel = CollisionGraphRoadModel.builder(GraphUtils.createGraph())
                .setVehicleLength(GraphUtils.VEHICLE_LENGTH)
                .build();

        agentFactory = new AgentFactory(agentFieldStrategy, roadModel, GraphUtils.VISUAL_RANGE, GraphUtils.getSpawnSites());
        parcelFactory = new ParcelFactory(parcelFieldStrategy, GraphUtils.getShelfSites(), GraphUtils.getDropOffSites());
        rndModel.register(agentFactory);
        rndModel.register(parcelFactory);

        sim = Simulator.builder()
                .addModel(rndModel)
                .addModel(commModel)
                .addModel(pdpModel)
                .addModel(roadModel)
                .addModel(new GradientModel())
                .build();

        parcelTracker = new ParcelTracker(treshold) {
            @Override
            public void deliveryTresholdReached() {
                sim.stop();

                System.out.println("Ran for " + sim.getCurrentTime() + "ms");

                try {
                    BufferedWriter fileWriter = new BufferedWriter(new FileWriter(fileOut));
                    try {
                        fileWriter.write("timeToPickup timeToDelivery timeOnRoute");
                        fileWriter.newLine();

                        Iterator<TimeAwareParcel> it = parcelTracker.iterator();
                        while (it.hasNext()) {
                            TimeAwareParcel p = it.next();
                            if (p.isDelivered()) {
                                long timeToPickup = p.getPickupTime() - p.getScheduleTime();
                                long timeToDelivery = p.getDeliveryTime() - p.getScheduleTime();
                                long timeOnRoute = p.getDeliveryTime() - p.getPickupTime();
                                fileWriter.write(String.format("%d %d %d", timeToPickup, timeToDelivery, timeOnRoute));
                                fileWriter.newLine();
                            }
                        }
                    } finally {
                        fileWriter.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        parcelScheduler = new ParcelScheduler(parcelFactory) {
            @Override
            public void generateParcel(TimeAwareParcel parcel) {
                sim.register(parcel);
                parcelTracker.track(parcel);
            }
        };
        sim.addTickListener(parcelScheduler);
        rndModel.register(parcelScheduler);
    }

    public void populate() {
        for (int i = 0; i < AGENTS; i++) {
            sim.register(agentFactory.makeAgent());
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

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("seed", true, "The random seed for the simulation.");
        options.addOption("treshold", true, "The parcel delivery treshold for the experiment.");
        options.addOption("output", true, "The output file for the simulation results.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cl = parser.parse(options, args);

        long seed = 123;
        if (cl.hasOption("seed")) {
            try {
                seed = Long.parseLong(cl.getOptionValue("seed"));
            } catch (NumberFormatException e) { }
        }

        int treshold = 100;
        if (cl.hasOption("treshold")) {
            try {
                treshold = Integer.parseInt(cl.getOptionValue("treshold"));
            } catch (NumberFormatException e) { }
        }

        String fileOut = "output.txt";
        if (cl.hasOption("output")) {
            fileOut = cl.getOptionValue("output");
        }

        System.out.printf("Seed: %d\n", seed);
        System.out.printf("Parcel treshold: %d\n", treshold);
        System.out.printf("Output file: %s\n", fileOut);

        Main main = new Main(seed, treshold, fileOut);
        main.populate();
        main.run();
    }

}

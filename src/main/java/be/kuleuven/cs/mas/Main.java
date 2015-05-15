package be.kuleuven.cs.mas;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;

public class Main {

    public static void main(String[] args) {
        final Simulator sim = Simulator.builder()
                .addModel(CollisionGraphRoadModel.builder(GraphUtils.createGraph())
                        .setVehicleLength(GraphUtils.VEHICLE_LENGTH)
                        .build())
                .build();

        for (int i = 0; i < 20; i++) {
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
                .show();
    }

}

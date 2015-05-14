package be.kuleuven.cs.mas;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.*;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class Main {

    private static final double VEHICLE_LENGTH = 2d;
    private static final int GRAPH_COLS = 8;
    private static final int GRAPH_ROWS = 7;
    private static final int VISUAL_RANGE = 2;

    static ImmutableTable<Integer, Integer, Point> createMatrix(int cols, int rows) {
        final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable.builder();
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                builder.put(r, c, new Point(c * VEHICLE_LENGTH * 2, r * VEHICLE_LENGTH * 2));
            }
        }
        return builder.build();
    }

    static ListenableGraph<LengthData> createGraph() {
        final Graph<LengthData> g = new TableGraph<>();

        final Table<Integer, Integer, Point> matrix = createMatrix(GRAPH_COLS, GRAPH_ROWS);

        for (int i = 0; i < matrix.columnMap().size(); i++) {
            Iterable<Point> path;
            if (i % 2 == 0) {
                path = Lists.reverse(newArrayList(matrix.column(i).values()));
            } else {
                path = matrix.column(i).values();
            }
            Graphs.addBiPath(g, path);
        }

        Graphs.addBiPath(g, matrix.row(0).values());
        Graphs.addBiPath(g, Lists.reverse(newArrayList(matrix.row(
                matrix.rowKeySet().size() - 2).values())));

        return new ListenableGraph<>(g);
    }

    static Collection<Point> getDropOffSites() {
        List<Point> sites = new ArrayList<>();
        for (int c = 0; c < GRAPH_COLS; c++) {
            sites.add(new Point(c * VEHICLE_LENGTH * 2, (GRAPH_ROWS - 1) * VEHICLE_LENGTH * 2));
        }
        return sites;
    }

    static Collection<Point> getShelfSites() {
        List<Point> sites = new ArrayList<>();
        for (int c = 0; c < GRAPH_COLS; c++) {
            for (int r = 1; r < GRAPH_ROWS - 1; r++) {
                sites.add(new Point(c * VEHICLE_LENGTH * 2, r * VEHICLE_LENGTH * 2));
            }
        }
        return sites;
    }

    public static void main(String[] args) {
        final Simulator sim = Simulator.builder()
                .addModel(CollisionGraphRoadModel.builder(createGraph())
                        .setVehicleLength(VEHICLE_LENGTH)
                        .build())
                .build();

        for (int i = 0; i < 20; i++) {
            sim.register(new AGVAgent(sim.getRandomGenerator(), VISUAL_RANGE));
        }

        View.create(sim)
                .with(GraphRoadModelRenderer.builder()
                                .setMargin((int) VEHICLE_LENGTH)
                                .showNodes()
                                .showDirectionArrows()
                )
                .with(AGVRenderer.builder()
                                .useDifferentColorsForVehicles()
                )
                .show();
    }

}

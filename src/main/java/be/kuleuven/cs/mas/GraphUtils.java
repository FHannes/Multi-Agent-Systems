package be.kuleuven.cs.mas;

import com.github.rinde.rinsim.geom.*;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.github.rinde.rinsim.geom.Graphs;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class GraphUtils {

    public static final double VEHICLE_LENGTH = 2d;
    public static final int GRAPH_COLS = 20;
    public static final int GRAPH_ROWS = 7;
    public static final int VISUAL_RANGE = 2;

    public static ImmutableTable<Integer, Integer, Point> createMatrix(int cols, int rows) {
        final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable.builder();
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                builder.put(r, c, new Point(c * VEHICLE_LENGTH * 2, r * VEHICLE_LENGTH * 2));
            }
        }
        return builder.build();
    }

    public static ListenableGraph<LengthData> createGraph() {
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
        Graphs.addBiPath(g, Lists.reverse(newArrayList(matrix.row(
                matrix.rowKeySet().size() - 1).values())));

        return new ListenableGraph<>(Graphs.unmodifiableGraph(g));
    }

    public static List<Point> getDropOffSites() {
        List<Point> sites = new ArrayList<>();
        for (int c = 0; c < GRAPH_COLS; c++) {
            sites.add(new Point(c * VEHICLE_LENGTH * 2, (GRAPH_ROWS - 1) * VEHICLE_LENGTH * 2));
        }
        return sites;
    }

    public static List<Point> getShelfSites() {
        List<Point> sites = new ArrayList<>();
        for (int c = 0; c < GRAPH_COLS; c++) {
            for (int r = 1; r < GRAPH_ROWS - 1; r++) {
                sites.add(new Point(c * VEHICLE_LENGTH * 2, r * VEHICLE_LENGTH * 2));
            }
        }
        return sites;
    }

    public static List<Point> getSpawnSites() {
        List<Point> sites = new ArrayList<>();
        for (int c = 0; c < GRAPH_COLS; c++) {
            for (int r = 0; r < GRAPH_ROWS; r++) {
                sites.add(new Point(c * VEHICLE_LENGTH * 2, r * VEHICLE_LENGTH * 2));
            }
        }
        return sites;
    }

}

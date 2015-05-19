package be.kuleuven.cs.mas.gradientfield.crawler;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;

import java.util.*;

public class GraphCrawler {

    private Graph<? extends ConnectionData> graph;

    public GraphCrawler(Graph<? extends ConnectionData> graph) {
        this.graph = graph;
    }

    public DistanceMap crawl(Point origin) {
        Queue<Point> queue = new LinkedList<>();
        queue.add(origin);
        Map<Point, Point> tree = new HashMap<>();
        tree.put(origin, origin);

        // Build pathing tree
        Point current;
        while ((current = queue.poll()) != null) {
            Collection<Point> incoming = graph.getIncomingConnections(current);
            for (Point source : incoming) {
                if (!tree.containsKey(source)) {
                    tree.put(source, current);
                    queue.add(source);
                } else {
                    // Check if the new path is shorter than the old one
                    double oldDistance = calculateDistance(tree, tree.get(source));
                    double newDistance = calculateDistance(tree, tree.get(current));
                    newDistance += Point.distance(source, current);
                    if (oldDistance > newDistance) {
                        tree.put(source, current);
                        queue.add(source);
                    }
                }
            }
        }

        // Calculate distance in each point
        Map<Point, Double> distanceMap = new HashMap<>();
        for (Point point : tree.keySet()) {
            distanceMap.put(point, calculateDistance(tree, point));
        }

        return new DistanceMap(distanceMap, origin);
    }

    private static double calculateDistance(Map<Point, Point> tree, Point point) {
        double distance = 0D;
        while (tree.containsKey(point)) {
            Point target = tree.get(point);
            distance += Point.distance(point, target);
            if (point == target) {
                break;
            }
            point = target;
        }
        return distance;
    }

}

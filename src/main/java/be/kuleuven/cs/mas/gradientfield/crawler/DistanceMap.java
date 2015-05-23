package be.kuleuven.cs.mas.gradientfield.crawler;

import com.github.rinde.rinsim.geom.Point;

import java.util.Map;

public class DistanceMap {

    private Map<Point, Double> map;
    private Point origin;
    private double maxDistance;

    public DistanceMap(Map<Point, Double> map, Point origin) {
        this.map = map;
        this.origin = origin;
        this.maxDistance = map.values().stream().max(Double::compare).get();
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public double getDistance(Point point) {
        if (map.containsKey(point)) {
            return map.get(point);
        }
        return 0D;
    }

    public Point getOrigin() {
        return origin;
    }

}

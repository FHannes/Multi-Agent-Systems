package be.kuleuven.cs.mas.gradientfield;

import be.kuleuven.cs.mas.gradientfield.crawler.DistanceMap;
import be.kuleuven.cs.mas.gradientfield.crawler.GraphCrawler;
import com.github.rinde.rinsim.core.model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class GradientModel extends AbstractModel<FieldEmitter> implements ModelReceiver {

    private Graph<? extends ConnectionData> graph;
    private GraphCrawler graphCrawler;

    private Set<FieldEmitter> emitters = new HashSet<>();
    private Map<Point, DistanceMap> distanceMaps = new HashMap<>();

    @Nullable
    private PDPModel pdpModel;

    public DistanceMap getDistanceMap(Point point) {
        if (distanceMaps.containsKey(point)) {
            return distanceMaps.get(point);
        }

        DistanceMap result = graphCrawler.crawl(point);
        distanceMaps.put(point, result);
        return result;
    }

    public synchronized double getGradient(Point point, Set<FieldEmitter> excludedEmitters) {
        double influence = 0D;
        for (FieldEmitter emitter : emitters) {
            if (excludedEmitters.contains(emitter)) {
                continue;
            }

            Point toMap = emitter.getPosition().get();
            
            if (! graph.containsNode(toMap)) {
            	double distance = Double.POSITIVE_INFINITY;
            	Point potPoint = null;
            	for (Point graphPoint : graph.getNodes()) {
            		if (Point.distance(point, graphPoint) < distance) {
            			potPoint = graphPoint;
            			distance = Point.distance(point, graphPoint);
            		}
            	}
            	toMap = potPoint;
            }
            
            DistanceMap distanceMap = getDistanceMap(toMap);
            double emitterInfluence = 1 - distanceMap.getDistance(point) / distanceMap.getMaxDistance();
            if (emitterInfluence < 0D) {
                emitterInfluence = 0D;
            }
            influence += emitterInfluence * emitter.getStrength();
        }
        return influence;
    }

    public double getGradient(Point point) {
        return getGradient(point, Collections.emptySet());
    }

    public Queue<Point> getGradientTargets(FieldEmitter emitter) {
        Set<FieldEmitter> exclusion = new HashSet<>();
        exclusion.add(emitter);

        Map<Point, Double> gradientValues = graph.getOutgoingConnections(emitter.getPosition().get()).stream().collect(
                Collectors.toMap(p -> p, p -> getGradient(p, exclusion))
        );

        return gradientValues.keySet().stream().sorted(
                (p1, p2) -> gradientValues.get(p1) - gradientValues.get(p2) < 0 ? -1 : 1
        ).collect(Collectors.toCollection(LinkedList::new));
    }

    public List<FieldEmitter> getEmitters() {
        return new ArrayList<>(emitters);
    }

    @Override
    public synchronized boolean register(FieldEmitter element) {
        emitters.add(element);
        element.setGradientModel(this);
        return true;
    }

    @Override
    public synchronized boolean unregister(FieldEmitter element) {
        element.setGradientModel(null);
        emitters.remove(element);
        return true;
    }

    @Override
    public void registerModelProvider(ModelProvider mp) {
        pdpModel = mp.tryGetModel(PDPModel.class);
        graph = mp.getModel(CollisionGraphRoadModel.class).getGraph();
        graphCrawler = new GraphCrawler(graph);
    }

}

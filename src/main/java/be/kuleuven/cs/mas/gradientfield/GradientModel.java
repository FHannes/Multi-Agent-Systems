package be.kuleuven.cs.mas.gradientfield;

import com.github.rinde.rinsim.core.model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GradientModel extends AbstractModel<FieldEmitter> implements ModelReceiver, EmitterListener {

    private Graph<? extends ConnectionData> graph;
    private Set<FieldEmitter> emitters = new HashSet<>();
    private Map<Point, InfluenceStore> pointEmitters = new HashMap<>();

    @Nullable
    private PDPModel pdpModel;

    private void addEmitter(FieldEmitter emitter) {
        emitters.add(emitter);
        emitter.addListener(this);
        updateEmitter(emitter);
    }

    private void clearEmitterInfluence(FieldEmitter emitter) {
        pointEmitters.values().forEach(store -> store.removeInfluence(emitter));
    }

    private void updateEmitter(FieldEmitter emitter) {
        clearEmitterInfluence(emitter);

        // TODO: Propagate emitter influence through the directional graph
    }

    private void removeEmitter(FieldEmitter emitter) {
        clearEmitterInfluence(emitter);
        emitters.remove(emitter);
    }

    @Override
    public boolean register(FieldEmitter element) {
        addEmitter(element);
        return true;
    }

    @Override
    public boolean unregister(FieldEmitter element) {
        removeEmitter(element);
        return true;
    }

    @Override
    public void registerModelProvider(ModelProvider mp) {
        pdpModel = mp.tryGetModel(PDPModel.class);
        graph = mp.getModel(CollisionGraphRoadModel.class).getGraph();
    }

    @Override
    public void onUpdatedPosition(FieldEmitter emitter) {
        updateEmitter(emitter);
    }

}

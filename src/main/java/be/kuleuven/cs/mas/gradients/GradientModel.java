package be.kuleuven.cs.mas.gradients;

import com.github.rinde.rinsim.core.model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.ModelReceiver;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class GradientModel extends AbstractModel<FieldEmitter> implements ModelReceiver {

    private Graph<? extends ConnectionData> graph;
    private Map<Point, EmitterStore> pointEmitters = new HashMap<>();

    @Nullable
    private PDPModel pdpModel;

    @Override
    public boolean register(FieldEmitter element) {
        return false;
    }

    @Override
    public boolean unregister(FieldEmitter element) {
        return false;
    }

    @Override
    public void registerModelProvider(ModelProvider mp) {
        pdpModel = mp.tryGetModel(PDPModel.class);
        graph = mp.getModel(CollisionGraphRoadModel.class).getGraph();
    }

}

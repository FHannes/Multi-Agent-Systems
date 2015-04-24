package be.kuleuven.cs.mas.gradients;

import java.util.Map;

/**
 * This class is responsible for storing all of the gradient field emitters that affect a specific point in the
 * {@link com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel}, which is used to similate the warehouse
 * environment in {@link be.kuleuven.cs.mas.Main}. The instances of this class are stored in the {@link GradientModel}
 * class instance.
 */
public class EmitterStore {

    private Map<FieldEmitter, Double> emitters;

    /**
     * Checks if {@code emitter} is already influencing the {@link com.github.rinde.rinsim.geom.Point} for which this
     * store holds the {@link FieldEmitter} instances.
     *
     * @param emitter
     *        The given {@link FieldEmitter} instance which is checked for.
     * @return True if {@code emitter} is already present in this instance.
     */
    public boolean hasEmitter(FieldEmitter emitter) {
        return emitters.keySet().contains(emitter);
    }

    public void addEmitter(FieldEmitter emitter, double influence) {
        assert !emitters.containsKey(emitter);

        emitters.put(emitter, influence);
    }

    public void setEmitterInfluence(FieldEmitter emitter, double influence) {
        emitters.put(emitter, influence);
    }

    public double getinfluence(FieldEmitter emitter) {
        if (hasEmitter(emitter)) {
            return emitters.get(emitter);
        } else {
            return 0;
        }
    }

    public double getFieldStrength() {
        return emitters.entrySet().stream().mapToDouble(e -> e.getKey().getStrength() * e.getValue()).sum();
    }

    public void removeEmitter(FieldEmitter emitter) {
        emitters.remove(emitter);
    }

}

package be.kuleuven.cs.mas.gradientfield;

import java.util.Map;

/**
 * This class is responsible for storing all of the gradient field emitters that affect a specific point in the
 * {@link com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel}, which is used to similate the warehouse
 * environment in {@link be.kuleuven.cs.mas.Main}. The instances of this class are stored in the {@link GradientModel}
 * class instance.
 */
public class InfluenceStore {

    private Map<FieldEmitter, Double> emitters;

    /**
     * Checks if {@code emitter} is already influencing the {@link com.github.rinde.rinsim.geom.Point} for which this
     * store holds the {@link FieldEmitter} influences.
     *
     * @param emitter
     *        The given {@link FieldEmitter} instance which is checked for.
     * @return True if {@code emitter} is already present in this instance.
     */
    public boolean hasInfluence(FieldEmitter emitter) {
        return emitters.keySet().contains(emitter);
    }

    /**
     * Sets the {@code influence} of a {@link FieldEmitter} at the point for which this instance stores the
     * {@link FieldEmitter} influences.
     *
     * @param emitter
     *        The given {@link FieldEmitter} instance for which the {@code influence} is being added or updated.
     * @param influence
     *        The influence strength of the given {@link FieldEmitter} {@code emitter}.
     */
    public void setInfluence(FieldEmitter emitter, double influence) {
        emitters.put(emitter, influence);
    }

    /**
     * Returns the strength of influence a {@link FieldEmitter} has on the {@link com.github.rinde.rinsim.geom.Point}
     * for which store holds the {@link FieldEmitter} influences.
     *
     * @param emitter
     *        The given {@link FieldEmitter} instance for which the {@code influence} is being returned.
     * @return The influence of the given {@link FieldEmitter} {@code emitter} or 0 if the {@code emitter} has no
     *         influence.
     */
    public double getInfluence(FieldEmitter emitter) {
        if (hasInfluence(emitter)) {
            return emitters.get(emitter);
        } else {
            return 0;
        }
    }

    /**
     * Calculates the total field strength in the {@link com.github.rinde.rinsim.geom.Point} for which this store holds
     * the {@link FieldEmitter} influences.
     *
     * @return The toptal field strength.
     */
    public double getFieldStrength() {
        return emitters.entrySet().stream().mapToDouble(e -> e.getKey().getStrength() * e.getValue()).sum();
    }

    /**
     * Removes the influence of a field emitter from the {@link com.github.rinde.rinsim.geom.Point} for which this store
     * holds the {@link FieldEmitter} influences.
     *
     * @param emitter
     *        The given {@link FieldEmitter} instance for which the {@code influence} is being removed.
     */
    public void removeInfluence(FieldEmitter emitter) {
        emitters.remove(emitter);
    }

}

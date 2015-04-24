package be.kuleuven.cs.mas.gradients;

import java.util.Map;

public class EmitterStore {

    private Map<FieldEmitter, Double> emitters;

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

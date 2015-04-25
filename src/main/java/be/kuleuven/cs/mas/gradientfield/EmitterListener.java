package be.kuleuven.cs.mas.gradientfield;

import java.util.EventListener;

public interface EmitterListener extends EventListener {

    void onUpdatedPosition(FieldEmitter emitter);

}

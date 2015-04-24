package be.kuleuven.cs.mas.gradientfield;

import com.github.rinde.rinsim.geom.Point;

/**
 * Thsi interface is implemented in all objects which emit a gradient field.
 */
public interface FieldEmitter {

    double getStrength();

    Point getPosition();

}

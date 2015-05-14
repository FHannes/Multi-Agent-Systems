package be.kuleuven.cs.mas.vision;

import be.kuleuven.cs.mas.modifiedclasses.CollisionGraphRoadModel;

import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public interface VisualSensorOwner {
	
	public Optional<Point> getPosition();
	
	public CollisionGraphRoadModel getRoadModel();

}

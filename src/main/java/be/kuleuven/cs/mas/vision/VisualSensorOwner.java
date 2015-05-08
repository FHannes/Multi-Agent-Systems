package be.kuleuven.cs.mas.vision;

import be.kuleuven.cs.mas.modifiedclasses.CollisionGraphRoadModel;

import com.github.rinde.rinsim.geom.Point;

public interface VisualSensorOwner {
	
	public Point getPosition();
	
	public CollisionGraphRoadModel getRoadModel();

}

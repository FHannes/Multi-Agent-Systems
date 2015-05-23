package be.kuleuven.cs.mas.vision;

import com.github.rinde.rinsim.core.model.road.CollisionGraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public interface VisualSensorOwner extends RoadUser {
	
	public Optional<Point> getPosition();
	
	public Point getMostRecentPosition();
	
	public CollisionGraphRoadModel getRoadModel();
	
	public Point getClosestGraphPoint();

}

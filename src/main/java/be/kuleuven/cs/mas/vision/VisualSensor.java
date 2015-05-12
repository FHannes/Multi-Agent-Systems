package be.kuleuven.cs.mas.vision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import be.kuleuven.cs.mas.modifiedclasses.CollisionGraphRoadModel;

import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

/**
 * Class representing an agent's visual capabilities. Can determine which points within the agent's visual range
 * are occupied by road users. The sensor assumes that the agent has a field of view of 360 degrees.
 * 
 * @author Thomas
 *
 */
public class VisualSensor {
	
	public VisualSensor(VisualSensorOwner owner, int visualRange) throws IllegalArgumentException {
		if (owner == null) {
			throw new IllegalArgumentException("owner cannot be null");
		}
		if (visualRange < 1) {
			throw new IllegalArgumentException("visualRange must be strictly positive");
		}
		
		this.owner = owner;
	}
	
	public Set<Point> getOccupiedPointsWithinVisualRange() {
		Set<Point> toReturn = new HashSet<>();
		Queue<PointQueueEntry> toProcess = new LinkedList<>();
		
		// initialise toProcess
		for (Point outgoing : this.getGraph().getOutgoingConnections(this.getPosition())) {
			toProcess.add(new PointQueueEntry(outgoing, this.getPosition(), 1, Direction.determineDirectionOf(this.getPosition(), outgoing)));
		}
		
		while (! toProcess.isEmpty()) {
			PointQueueEntry toCheck = toProcess.poll();
			boolean added = false;
			
			// check if a RoadUser occupies the point or is driving towards it from the previous point
			// with respect to the point of view of the VisualSensorOwner
			if (this.getRoadModel().hasRoadUserOnIgnoreFrom(toCheck.getFromPoint(), toCheck.getPoint())) {
				toReturn.add(toCheck.getPoint());
				added = true;
			}
			
			// check if a RoadUser is driving towards the point from the next point in the considered direction
			Optional<Point> otherEnd = this.getConnectedPointInDirection(toCheck.getPoint(), toCheck.getDirection());
			if (otherEnd.isPresent() && this.getRoadModel().hasRoadUserOnIgnoreFromAndTo(otherEnd.get(), toCheck.getPoint())) {
				if (! added) {
					toReturn.add(toCheck.getPoint());
				}
			}
			
			// if the visual range limit has not yet been reached and no RoadUser has been seen 
			// (we assume that the VisualSensor cannot see past RoadUsers), enqueue otherEnd
			if (! added && toCheck.getRange() < this.getVisualRange() && otherEnd.isPresent()) {
				toProcess.add(new PointQueueEntry(otherEnd.get(), toCheck.getPoint(), toCheck.getRange() + 1, toCheck.getDirection()));
			}
		}
		return toReturn;
	}
	
	private VisualSensorOwner owner;
	
	private VisualSensorOwner getOwner() {
		return this.owner;
	}
	
	private Point getPosition() {
		return this.getOwner().getPosition();
	}
	
	private CollisionGraphRoadModel getRoadModel() {
		return this.getOwner().getRoadModel();
	}
	
	private Graph<? extends ConnectionData> getGraph() {
		return this.getOwner().getRoadModel().getGraph();
	}
	
	private int visualRange;
	
	private int getVisualRange() {
		return this.visualRange;
	}
	
	private Optional<Point> getConnectedPointInDirection(Point reference, Direction direction) {
		for (Point point : this.getGraph().getOutgoingConnections(reference)) {
			if (Direction.determineDirectionOf(reference, point).equals(direction)) {
				return Optional.of(point);
			}
		}
		
		return Optional.absent(); 
	}
	
	private class PointQueueEntry {
		
		PointQueueEntry(Point point, Point from, int range, Direction direction) {
			this.point = point;
			this.range = range;
		}
		
		private Point point;
		private Point from;
		private int range;
		private Direction direction;
		
		Point getPoint() {
			return this.point;
		}
		
		Point getFromPoint() {
			return this.from;
		}
		
		int getRange() {
			return this.range;
		}
		
		Direction getDirection() {
			return this.direction;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((direction == null) ? 0 : direction.hashCode());
			result = prime * result + ((from == null) ? 0 : from.hashCode());
			result = prime * result + ((point == null) ? 0 : point.hashCode());
			result = prime * result + range;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PointQueueEntry other = (PointQueueEntry) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (direction != other.direction)
				return false;
			if (from == null) {
				if (other.from != null)
					return false;
			} else if (!from.equals(other.from))
				return false;
			if (point == null) {
				if (other.point != null)
					return false;
			} else if (!point.equals(other.point))
				return false;
			if (range != other.range)
				return false;
			return true;
		}

		private VisualSensor getOuterType() {
			return VisualSensor.this;
		}
	}

}

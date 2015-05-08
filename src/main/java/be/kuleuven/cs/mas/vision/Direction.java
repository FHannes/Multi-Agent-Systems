package be.kuleuven.cs.mas.vision;

import com.github.rinde.rinsim.geom.Point;

public enum Direction {
	NORTH, NORTHEAST, EAST, SOUTHEAST, SOUTH, SOUTHWEST, WEST, NORTHWEST, EQUAL;
	
	public static Direction determineDirectionOf(Point reference, Point target) {
		double diffX = reference.x - target.x;
		double diffY = reference.y - target.y;
		
		if (diffX == 0 && diffY > 0) {
			return NORTH;
		} else if (diffX < 0 && diffY > 0) {
			return NORTHEAST;
		} else if (diffX < 0 && diffY == 0) {
			return EAST;
		} else if (diffX < 0 && diffY < 0) {
			return SOUTHEAST;
		} else if (diffX == 0 && diffY < 0) {
			return SOUTH;
		} else if (diffX > 0 && diffY < 0) {
			return SOUTHWEST;
		} else if (diffX > 0 && diffY == 0) {
			return WEST;
		} else if (diffX > 0 && diffY > 0) {
			return NORTHWEST;
		} else {
			return EQUAL;
		}
	}
}
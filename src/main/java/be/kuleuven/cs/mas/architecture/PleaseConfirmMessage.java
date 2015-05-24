package be.kuleuven.cs.mas.architecture;

import java.util.Set;

import com.github.rinde.rinsim.geom.Point;

public class PleaseConfirmMessage {
	
	private String requester;
	private String propagator;
	private long timeStamp;
	private Set<Point> confirmPositions;
	
	public PleaseConfirmMessage(String requester, String propagator,
			long timeStamp, Set<Point> confirmPositions) {
		super();
		this.requester = requester;
		this.propagator = propagator;
		this.timeStamp = timeStamp;
		this.confirmPositions = confirmPositions;
	}

	public String getRequester() {
		return requester;
	}
	
	public String getPropagator() {
		return propagator;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public Set<Point> getConfirmPositions() {
		return confirmPositions;
	}

}

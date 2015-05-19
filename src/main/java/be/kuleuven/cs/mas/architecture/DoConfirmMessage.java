package be.kuleuven.cs.mas.architecture;

import com.github.rinde.rinsim.geom.Point;

public class DoConfirmMessage {
	
	private String requester;
	private String propagator;
	private long timeStamp;
	private Point wantPos;
	
	public DoConfirmMessage(String requester, String propagator,
			long timeStamp, Point wantPos) {
		super();
		this.requester = requester;
		this.propagator = propagator;
		this.timeStamp = timeStamp;
		this.wantPos = wantPos;
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

	public Point getWantPos() {
		return wantPos;
	}

}

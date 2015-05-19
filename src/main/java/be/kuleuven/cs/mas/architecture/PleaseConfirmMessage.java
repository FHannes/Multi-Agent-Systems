package be.kuleuven.cs.mas.architecture;

import com.github.rinde.rinsim.geom.Point;

public class PleaseConfirmMessage {
	
	private String requester;
	private String propagator;
	private long timeStamp;
	private Point wantPos;
	
	public PleaseConfirmMessage(String requester, String propagator,
			long timeStamp, Point wantPos) {
		super();
		this.requester = requester;
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

package be.kuleuven.cs.mas.architecture;

import java.util.List;

import com.github.rinde.rinsim.geom.Point;

public class MoveAsideMessage {

	private String requester;
	private String propagator;
	private List<String> waitForList;
	private long timeStamp;
	private long parcelWaitingSince;
	private Point wantPos;
	private Point atPos;
	private int step;
	
	public MoveAsideMessage(String requester, String propagator,
			List<String> waitForList, long timeStamp, long parcelWaitingSince,
			Point wantPos, Point atPos, int step) {
		super();
		this.requester = requester;
		this.propagator = propagator;
		this.waitForList = waitForList;
		this.timeStamp = timeStamp;
		this.parcelWaitingSince = parcelWaitingSince;
		this.wantPos = wantPos;
		this.atPos = atPos;
		this.step = step;
	}
	
	public String getRequester() {
		return requester;
	}
	public String getPropagator() {
		return propagator;
	}
	public List<String> getWaitForList() {
		return waitForList;
	}
	public long getTimeStamp() {
		return timeStamp;
	}
	public long getParcelWaitingSince() {
		return parcelWaitingSince;
	}
	public Point getWantPos() {
		return wantPos;
	}
	public Point getAtPos() {
		return atPos;
	}
	public int getStep() {
		return step;
	}
}

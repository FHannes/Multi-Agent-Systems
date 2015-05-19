package be.kuleuven.cs.mas.architecture;

public class AckMessage {

	private String requester;
	private String propagator;
	private long timeStamp;
	
	public AckMessage(String requester, String propagator, long timeStamp) {
		super();
		this.requester = requester;
		this.propagator = propagator;
		this.timeStamp = timeStamp;
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
}

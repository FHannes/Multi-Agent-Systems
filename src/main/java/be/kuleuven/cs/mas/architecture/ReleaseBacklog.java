package be.kuleuven.cs.mas.architecture;

import java.util.List;

public class ReleaseBacklog {
	
	private String requester;
	private long timeStamp;
	private List<String> propagators;
	
	public ReleaseBacklog(String requester, long timeStamp,
			List<String> propagators) {
		super();
		this.requester = requester;
		this.timeStamp = timeStamp;
		this.propagators = propagators;
	}
	
	public String getRequester() {
		return requester;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public List<String> getPropagators() {
		return propagators;
	}
	
	public boolean isEmpty() {
		return this.getPropagators().isEmpty();
	}

}

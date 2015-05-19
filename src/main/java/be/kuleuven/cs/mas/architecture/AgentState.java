package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.agent.AGVAgent;
import be.kuleuven.cs.mas.message.AgentMessage;
import be.kuleuven.cs.mas.message.Field;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface representing the state of an agent. The state determines the behaviour of the agent when told to act
 * or when receiving a message
 * 
 * @author Thomas
 *
 */
public abstract class AgentState {
	
	public static final long RESEND_TIMEOUT = 5000;
	protected static final Pattern NUM_PATTERN = Pattern.compile("\\d+");
	protected static final String DEADLOCK_SEP = ",";
	
	protected AGVAgent agent;
	private List<ReleaseBacklog> backLogs = new LinkedList<>();
	
	public AgentState(AGVAgent agent) {
		this.agent = agent;
	}
	
	protected AgentState(AGVAgent agent, List<ReleaseBacklog> backLogs) {
		this(agent);
		this.backLogs = backLogs;
	}
	
	/**
	 * Performs one or more actions appropriate to the state
	 */
	public abstract void act(TimeLapse timeLapse);
	
	/**
	 * Processes the given message in a manner appropriate to the state
	 */
	public void processMessage(AgentMessage msg) {
		List<Field> contents = msg.getContents();
		switch(contents.get(0).getName()) {
		case "move-aside": Optional<MoveAsideMessage> moveMessage = parseMoveAsideMessage(contents);
		if (moveMessage.isPresent()) {
			this.processMoveAsideMessage(moveMessage.get());
		}
		break;
		case "release": Optional<ReleaseMessage> releaseMessage = parseReleaseMessage(contents);
		if (releaseMessage.isPresent()) {
			this.processReleaseMessage(releaseMessage.get());
		}
		break;
		case "home-free": Optional<HomeFreeMessage> homeMessage = parseHomeFreeMessage(contents);
		if (homeMessage.isPresent()) {
			this.processHomeFreeMessage(homeMessage.get());
			break;
		}
		break;
		case "reject": Optional<RejectMessage> rejectMessage = parseRejectMessage(contents);
		if (rejectMessage.isPresent()) {
			this.processRejectMessage(rejectMessage.get());
			break;
		}
		break;
		case "ack": Optional<AckMessage> ackMessage = parseAckMessage(contents);
		if (ackMessage.isPresent()) {
			this.processAckMessage(ackMessage.get());
		}
		break;
		default: return;
		}
	}
	
	protected static Optional<MoveAsideMessage> parseMoveAsideMessage(List<Field> contents) {
		int i = 1;
		if (! contents.get(i).getName().equals("requester")) {
			return Optional.absent();
		}
		String requester = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("propagator")) {
			return Optional.absent();
		}
		String propagator = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("wait-for")) {
			return Optional.absent();
		}
		List<String> waitForList = toWaitForList(contents.get(i++).getValue());
		if (! contents.get(i).getName().equals("timestamp")) {
			return Optional.absent();
		}
		long timeStamp = Long.parseLong(contents.get(i++).getValue());
		if (! contents.get(i).getName().equals("parcel-waiting-since")) {
			return Optional.absent();
		}
		long parcelWaitingSince = Long.parseLong(contents.get(i++).getValue());
		if (! contents.get(i).getName().equals("want-pos")) {
			return Optional.absent();
		}
		Point wantPos = Point.parsePoint(contents.get(i++).getValue());
		if (! contents.get(i).getName().equals("at-pos")) {
			return Optional.absent();
		}
		Point atPos = Point.parsePoint(contents.get(i++).getValue());
		if (! contents.get(i).getName().equals("step")) {
			return Optional.absent();
		}
		int step = Integer.parseInt(contents.get(i++).getValue());
		return Optional.of(new MoveAsideMessage(requester, propagator, waitForList, timeStamp, parcelWaitingSince, wantPos, atPos, step));
	}
	
	protected abstract void processMoveAsideMessage(MoveAsideMessage msg);
	
	protected void sendMoveAside(String requester, List<String> waitFor, long timeStamp, long parcelWaitingSince,
			Point wantPos, int step) {
		this.getAgent().getMessageBuilder().addField("move-aside")
		.addField("requester", requester)
		.addField("propagator", this.getAgent().getName())
		.addField("wait-for", toWaitForString(waitFor))
		.addField("timestamp", Long.toString(timeStamp))
		.addField("parcel-waiting-since", Long.toString(parcelWaitingSince))
		.addField("want-pos", wantPos.toString())
		.addField("at-pos", this.getAgent().getMostRecentPosition().toString())
		.addField("step", Integer.toString(step));
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().build());
	}
	
	protected static Optional<ReleaseMessage> parseReleaseMessage(List<Field> contents) {
		int i = 1;
		if (! contents.get(i).getName().equals("requester")) {
			return Optional.absent();
		}
		String requester = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("propagator")) {
			return Optional.absent();
		}
		String propagator = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("timestamp")) {
			return Optional.absent();
		}
		long timeStamp = Long.parseLong(contents.get(i++).getValue());
		return Optional.of(new ReleaseMessage(requester, propagator, timeStamp));
	}
	
	// MUST OVERRIDE
	protected void processReleaseMessage(ReleaseMessage msg) {
		Iterator<ReleaseBacklog> it = this.getBackLogs().iterator();
		while (it.hasNext()) {
			ReleaseBacklog log = it.next();
			if (log.getRequester().equals(msg.getRequester()) && log.getPropagators().contains(msg.getPropagator())) {
				this.sendRelease(log.getRequester(), log.getTimeStamp());
				if (log.isEmpty()) {
					it.remove();
				}
			}
		}
	}
	
	protected void sendRelease(String requester, long timeStamp) {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("release")
				.addField("requester", requester)
				.addField("propagator", this.getAgent().getName())
				.addField("timestamp", Long.toString(timeStamp))
				.build());
	}
	
	protected static Optional<HomeFreeMessage> parseHomeFreeMessage(List<Field> contents) {
		int i = 1;
		if (! contents.get(i).getName().equals("requester")) {
			return Optional.absent();
		}
		String requester = contents.get(i++).getValue();
		return Optional.of(new HomeFreeMessage(requester));
	}
	
	protected abstract void processHomeFreeMessage(HomeFreeMessage msg);
	
	protected void sendHomeFree() {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("home-free")
				.addField("requester", this.getAgent().getName()).build());
	}
	
	protected static Optional<RejectMessage> parseRejectMessage(List<Field> contents) {
		int i = 1;
		if (! contents.get(i).getName().equals("requester")) {
			return Optional.absent();
		}
		String requester = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("propagator")) {
			return Optional.absent();
		}
		String propagator = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("timestamp")) {
			return Optional.absent();
		}
		Long timeStamp = Long.parseLong(contents.get(i++).getValue());
		return Optional.of(new RejectMessage(requester, propagator, timeStamp));
	}
	
	protected abstract void processRejectMessage(RejectMessage msg);
	
	protected void sendReject(String requester, String propagator, long timeStamp) {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("reject")
				.addField("requester", requester)
				.addField("propagator", propagator)
				.addField("timestamp", Long.toString(timeStamp))
				.build());
	}
	
	protected static Optional<AckMessage> parseAckMessage(List<Field> contents) {
		int i = 1;
		if (! contents.get(i).getName().equals("requester")) {
			return Optional.absent();
		}
		String requester = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("propagator")) {
			return Optional.absent();
		}
		String propagator = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("timestamp")) {
			return Optional.absent();
		}
		long timeStamp = Long.parseLong(contents.get(i++).getValue());
		return Optional.of(new AckMessage(requester, propagator, timeStamp));
	}
	
	protected void sendAck(String requester, String propagator, long timeStamp) {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("ack")
				.addField("requester", requester)
				.addField("propagator", propagator)
				.addField("timestamp", Long.toString(timeStamp))
				.build());
	}
	
	protected abstract void processAckMessage(AckMessage msg);
	
	protected AGVAgent getAgent() {
		return this.agent;
	}
	
	protected void sendMessage(AgentMessage message) {
		this.getAgent().sendMessage(message);
	}
	
	protected List<ReleaseBacklog> getBackLogs() {
		return this.backLogs;
	}
	
	public abstract void uponSet();
	
	/**
	 * Determine what the next state of the agent should be, either from internal information
	 * or the given recommended next state
	 */
	protected abstract void doStateTransition(Optional<AgentState> nextState);
	
	protected static boolean trafficPriorityFunction(String oneReq, String otherReq, long oneWaitTime, long otherWaitTime) {
		int compare = Long.compare(oneWaitTime, otherWaitTime);

		if (compare < 0) {
			return true;
		} else if (compare > 0) {
			return false;
		} else {
			Matcher oneReqMatcher = AgentState.NUM_PATTERN.matcher(oneReq);
			Matcher otherMatcher = NUM_PATTERN.matcher(otherReq);

			if (! oneReqMatcher.find()) {
				throw new IllegalArgumentException("oneReq does not conform to agent name conventions");
			}
			if (! otherMatcher.find()) {
				throw new IllegalStateException("otherReq does not conform to agent name conventions");
			}

			// equality can never occur since agents cannot have the same number
			return Integer.parseInt(otherMatcher.group()) < Integer.parseInt(oneReqMatcher.group());
		}

	}
	
	protected static List<String> toWaitForList(String waitForString) {
		return new LinkedList<>(Arrays.asList(waitForString.split(DEADLOCK_SEP)));
	}
	
	protected static String toWaitForString(List<String> waitForList) {
		StringBuilder toReturn = new StringBuilder();
		for (String ele : waitForList) {
			toReturn.append(ele);
			toReturn.append(DEADLOCK_SEP);
		}
		return toReturn.toString();
	}
	
	protected boolean hasDeadlock(List<String> waitForList) {
		return waitForList.contains(this.getAgent().getName());
	}

	/**
	 * Returns the strength of the agent's influence on the gradient field. This value has to be positive as the agent
	 * emits a repulsion field.
	 */
	public abstract double getFieldStrength();

}

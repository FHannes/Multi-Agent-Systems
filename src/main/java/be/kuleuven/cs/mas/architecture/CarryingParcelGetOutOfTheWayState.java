package be.kuleuven.cs.mas.architecture;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.kuleuven.cs.mas.AGVAgent;
import be.kuleuven.cs.mas.message.AgentMessage;
import be.kuleuven.cs.mas.message.Field;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

public class CarryingParcelGetOutOfTheWayState extends CarryingParcelState {

	private String requester;
	private String propagator;
	private long parcelWaitTime;
	private int step;
	private Point nextWantedPoint;
	private boolean waitingOnOther = false;
	private long timeOutCount = 0;

	public CarryingParcelGetOutOfTheWayState(AGVAgent agent, String requester, String propagator, long parcelWaitTime, int step, Point propagatorPos) {
		super(agent);
		this.requester = requester;
		this.propagator = propagator;
		this.parcelWaitTime = parcelWaitTime;
		this.step = step;
		
		this.doMoveAside(propagatorPos);
	}

	@Override
	public void act(TimeLapse timeLapse) {
		// first of all, check if the parcel can be delivered
		if (this.getAgent().getPosition().equals(this.getAgent().getParcel().get().getDestination())) {
			this.getAgent().getPDPModel().deliver(this.getAgent(), this.getAgent().getParcel().get(), timeLapse);
			// if parcel is not in cargo anymore, delivery was successful
			if (! this.getAgent().getPDPModel().containerContains(this.getAgent(), this.getAgent().getParcel().get())) {
				// parcel has been delivered, so follow gradient field now
				this.getAgent().getParcel().get().notifyDelivered(timeLapse);
				this.doStateTransition(Optional.of(new FollowGradientFieldState(this.getAgent())));
			}
			return;
		}
		
		boolean wasAlreadyWaiting = this.isWaitingOnOther();
		if (wasAlreadyWaiting) {
			this.setTimeOutCount(this.getTimeOutCount() + timeLapse.getStartTime());
		}
		Set<Point> occupiedPoints = this.getAgent().getOccupiedPointsInVisualRange();
		if (! occupiedPoints.contains(this.getNextWantedPoint())) {
			// we can indeed move forward
			this.getAgent().followPath(timeLapse);
			if (this.getAgent().getPosition().equals(this.getNextWantedPoint())) {
				// move forward has succeeded
				this.setWaitingOnOther(false);
				return;
			}
		}

		this.setWaitingOnOther(true);
		if (this.timeOutOccurred() || !wasAlreadyWaiting) {
			// resend move-aside
			this.resendMoveAside();
		}
	}

	@Override
	public void processMessage(AgentMessage msg) {
		switch(msg.getContents().get(0).getName()) {
		case "move-aside": this.processMoveAside(msg);
		break;
		case "reject": this.processRejectMessage(msg);
		break;
		case "ack": this.processAckMessage(msg);
		break;
		case "release": this.processReleaseMessage(msg);
		break;
		case "home-free": this.processHomeFreeMessage(msg);
		break;
		default: return;
		}
	}

	@Override
	public void uponSet() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doStateTransition(Optional<AgentState> nextState) {
		this.getAgent().setAgentState(nextState.get());
	}
	
	private void resendMoveAside() {
		this.getAgent().getMessageBuilder().addField("move-aside")
		.addField("requester", this.getRequester())
		.addField("propagator", this.getAgent().getName())
		.addField("parcel-waiting-since", Long.toString(this.getParcelWaitTime()))
		.addField("want-pos", this.getNextWantedPoint().toString())
		.addField("at-pos", this.getAgent().getMostRecentPosition().toString())
		.addField("step", Integer.toString(this.getStep()));
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().build());
		this.setTimeOutCount(0);
	}
	
	private void sendAck(String requester, String propagator) {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("ack")
				.addField("requester", requester)
				.addField("propagator", propagator)
				.build());
	}
	
	private void sendReject(String requester, String propagator) {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("reject")
				.addField("requester", requester)
				.addField("propagator", propagator)
				.build());
	}
	
	private void sendRelease() {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("release")
				.addField("requester", this.getRequester())
				.addField("propagator", this.getAgent().getName())
				.build());
	}
	
	private void processMoveAside(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("propagator")) {
			return;
		}
		String propagator = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("parcel-waiting-since")) {
			return;
		}
		long parcelWaitingSince = Long.parseLong(contents.get(i++).getValue());
		if (! contents.get(i).getName().equals("want-pos")) {
			return;
		}
		Point requestedPoint = Point.parsePoint(contents.get(i++).getValue());
		if (! (this.getAgent().getPosition().equals(requestedPoint)
				|| this.getAgent().getNextPointOnPath().equals(requestedPoint))) {
			return;
		}
		if (! contents.get(i).getName().equals("at-pos")) {
			return;
		}
		Point propagatorPos = Point.parsePoint(contents.get(i++).getValue());
		if (! contents.get(i).getName().equals("step")) {
			// invalid message, so ignore
			return;
		}
		if (requester.equals(this.getRequester())) {
			// check if the step has increased, in which case the agent must once again move out of the way
			int step = Integer.parseInt(contents.get(i).getValue());
			if (step > this.getStep()) {
				this.doMoveAside(propagatorPos);
				this.setPropagator(propagator);
				this.setStep(step);
				return;
			}
		}
		if (this.trafficPriorityFunction(this.getRequester(), parcelWaitingSince)) {
			this.sendReject(requester, propagator);
		} else {
			this.sendAck(requester, propagator);
			int step = Integer.parseInt(contents.get(i).getValue());
			this.setRequester(requester);
			this.setPropagator(propagator);
			this.setParcelWaitTime(parcelWaitingSince);
			this.setStep(step);
			this.setTimeOutCount(0);
			this.setWaitingOnOther(false);
			this.sendRelease();
			this.doMoveAside(propagatorPos);
		}
	}
	

	private void processRejectMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! requester.equals(this.getRequester())) {
			return;
		}
		if (! contents.get(i).getName().equals("propagator")) {
			return;
		}
		String propagator = contents.get(i++).getValue();
		if (! propagator.equals(this.getAgent().getName())) {
			return;
		}
		// reset time-out
		this.setTimeOutCount(0);
	}

	private void processAckMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! requester.equals(this.getRequester())) {
			return;
		}
		if (! contents.get(i).getName().equals("propagator")) {
			return;
		}
		String propagator = contents.get(i++).getValue();
		if (! propagator.equals(this.getAgent().getName())) {
			return;
		}
		// reset time-out
		this.setTimeOutCount(0);
	}
	
	private void processReleaseMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! requester.equals(this.getRequester())) {
			return;
		}
		if (! contents.get(i).getName().equals("propagator")) {
			return;
		}
		String propagator = contents.get(i++).getValue();
		if (! propagator.equals(this.getPropagator())) {
			return;
		}
		// the propagator has released this agent from coordinating the traffic jam
		this.doStateTransition(Optional.of(new CarryingParcelNoJamState(this.getAgent(), true)));
	}
	
	private void processHomeFreeMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i).getValue();
		if (requester.equals(this.getRequester())) {
			this.doStateTransition(Optional.of(new CarryingParcelNoJamState(this.getAgent(), true)));
		}
	}
	
	private void doMoveAside(Point forbiddenPoint) {
		Optional<Point> nextStep = agent.getRandomReachablePoint(new HashSet<Point>(Arrays.asList(forbiddenPoint)));
		if (nextStep.isPresent()) {
			this.setNextWantedPoint(nextStep.get());
		} else {
			nextStep = agent.getRandomNeighbourPoint(new HashSet<Point>(Arrays.asList(forbiddenPoint)));
			// we assume that every point has a neighbour
			this.setNextWantedPoint(nextStep.get());
		}
	}
	
	private String getRequester() {
		return requester;
	}

	private void setRequester(String requester) {
		this.requester = requester;
	}
	
	private String getPropagator() {
		return this.propagator;
	}
	
	private void setPropagator(String propagator) {
		this.propagator = propagator;
	}

	private long getParcelWaitTime() {
		return parcelWaitTime;
	}

	private void setParcelWaitTime(long parcelWaitTime) {
		this.parcelWaitTime = parcelWaitTime;
	}

	private int getStep() {
		return step;
	}

	private void setStep(int step) {
		this.step = step;
	}

	private Point getNextWantedPoint() {
		return nextWantedPoint;
	}

	private void setNextWantedPoint(Point nextWantedPoint) {
		this.nextWantedPoint = nextWantedPoint;
	}

	private boolean isWaitingOnOther() {
		return waitingOnOther;
	}

	private void setWaitingOnOther(boolean waitingOnOther) {
		this.waitingOnOther = waitingOnOther;
	}

	private long getTimeOutCount() {
		return timeOutCount;
	}

	private void setTimeOutCount(long timeOutCount) {
		this.timeOutCount = timeOutCount;
	}

	private boolean timeOutOccurred() {
		return this.getTimeOutCount() >= AgentState.RESEND_TIMEOUT;
	}
}
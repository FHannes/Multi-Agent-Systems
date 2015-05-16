package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.AGVAgent;
import be.kuleuven.cs.mas.message.AgentMessage;
import be.kuleuven.cs.mas.message.Field;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.List;
import java.util.Set;

public class CarryingParcelControllingJamState extends CarryingParcelState {

	private int step = 0;
	private long timeOutCount = 0;
	private Point nextWantedPoint;

	public CarryingParcelControllingJamState(AGVAgent agent) {
		super(agent);
		this.nextWantedPoint = agent.getNextPointOnPath().get();
	}

	@Override
	public void act(TimeLapse timeLapse) {
		// if it so happens that we can deliver the parcel, let everyone know and deliver it
		if (this.getAgent().getParcel().get().getDestination().equals(this.getAgent().getPosition())) {
			this.tryDeliverParcel(timeLapse);
			return;
		}

		this.setTimeOutCount(this.getTimeOutCount() + timeLapse.getStartTime());
		// if there was a time-out, first try to move to the next wanted point
		Set<Point> occupiedPoints = this.getAgent().getOccupiedPointsInVisualRange();
		if (! occupiedPoints.contains(this.getNextWantedPoint())) {
			// we can indeed move forward
			this.getAgent().followPath(timeLapse);
			if (this.getAgent().getPosition().equals(this.getNextWantedPoint())) {
				// move forward has succeeded
				this.afterMoveForward();
				return;
			}
		}

		if (this.timeOutOccurred()) {
			// resend move-aside
			this.resendMoveAside();
		}
	}

	@Override
	public void processMessage(AgentMessage msg) {
		switch (msg.getContents().get(0).getName()) {
		case "move-aside": this.processMoveAsideMessage(msg);
		break;
		case "reject": this.processRejectMessage(msg);
		break;
		case "ack": this.processAckMessage(msg);
		break;
		default: return; // ignore all other messages
		}
	}

	private void afterMoveForward() {
		this.setStep(this.getStep() + 1);

		this.setNextWantedPoint(this.getAgent().getNextPointOnPath().get());
		if (! this.getAgent().occupiedPointsOnPathWithinRange()) {
			// we are home free
			this.sendHomeFree();
			this.doStateTransition(Optional.of(new CarryingParcelNoJamState(this.getAgent(), false)));
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
		.addField("requester", this.getAgent().getName())
		.addField("propagator", this.getAgent().getName())
		.addField("wait-for", this.getAgent().getName())
		.addField("parcel-waiting-since", Long.toString(this.getAgent().getParcel().get().getWaitingSince()))
		.addField("want-pos", this.getNextWantedPoint().toString())
		.addField("at-pos", this.getAgent().getMostRecentPosition().toString())
		.addField("step", Integer.toString(this.getStep()));
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().build());
		this.setTimeOutCount(0);
	}
	
	private void sendHomeFree() {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("home-free")
				.addField("requester", this.getAgent().getName()).build());
	}

	private void tryDeliverParcel(TimeLapse timeLapse) {
		if (! (this.getAgent().getPDPModel().getParcelState(this.getAgent().getParcel().get()) == ParcelState.DELIVERING)) {
			// we have not yet started delivering the parcel, so send the home-free message
			this.sendHomeFree();
		}
		this.getAgent().getPDPModel().deliver(this.getAgent(), this.getAgent().getParcel().get(), timeLapse);
		if (! this.getAgent().getPDPModel().containerContains(this.getAgent(), this.getAgent().getParcel().get())) {
			// parcel delivered, so start following the gradient field
			this.getAgent().getParcel().get().notifyDelivered(timeLapse);
			this.doStateTransition(Optional.of(new FollowGradientFieldState(this.getAgent())));
		}
	}

	private int getStep() {
		return this.step;
	}

	private void setStep(int step) {
		this.step = step;
	}

	private long getTimeOutCount() {
		return this.timeOutCount;
	}

	private void setTimeOutCount(long timeOutCount) {
		this.timeOutCount = Math.min(this.getTimeOutCount(), AgentState.RESEND_TIMEOUT);
	}

	private boolean timeOutOccurred() {
		return this.getTimeOutCount() >= AgentState.RESEND_TIMEOUT;
	}

	private Point getNextWantedPoint() {
		return this.nextWantedPoint;
	}

	private void setNextWantedPoint(Point point) {
		this.nextWantedPoint = point;
	}

	private void processMoveAsideMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		boolean sawOwnName = false;
		
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (requester.equals(this.getAgent().getName())) {
			// monitor for possible deadlock
			sawOwnName = true;
			return;
		}
		if (! contents.get(i).getName().equals("propagator")) {
			return;
		}
		String propagator = contents.get(i++).getValue();
		if (propagator.equals(this.getAgent().getName())) {
			return;
		}
		if (! contents.get(i).getName().equals("wait-for")) {
			return;
		}
		List<String> waitForList = toWaitForList(contents.get(i++).getValue());
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
		if (sawOwnName) {
			// deadlock has occurred, restart protocol
			this.sendHomeFree();
			this.resendMoveAside();
		}
		if (! contents.get(i).getName().equals("at-pos")) {
			return;
		}
		Point propagatorPos = Point.parsePoint(contents.get(i++).getValue());
		if (! contents.get(i).getName().equals("step")) {
			// invalid message, so ignore
			return;
		}
		if (this.trafficPriorityFunction(requester, parcelWaitingSince)) {
			this.getAgent().getMessageBuilder().addField("reject")
			.addField("requester", requester)
			.addField("propagator", propagator);
			this.getAgent().sendMessage(this.getAgent().getMessageBuilder().build());
		} else {
			this.getAgent().getMessageBuilder().addField("ack")
			.addField("requester", requester)
			.addField("propagator", propagator);
			this.getAgent().sendMessage(this.getAgent().getMessageBuilder().build());
			int step = Integer.parseInt(contents.get(i).getValue());
			// release all agents waiting on this agent
			this.sendHomeFree();
			this.doStateTransition(Optional.of(new CarryingParcelGetOutOfTheWayState(this.getAgent(), requester, propagator, waitForList, parcelWaitingSince, step, propagatorPos)));
		}
	}

	private void processRejectMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! requester.equals(this.getAgent().getName())) {
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

	// TODO are ack messages necessary?
	private void processAckMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! requester.equals(this.getAgent().getName())) {
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
}

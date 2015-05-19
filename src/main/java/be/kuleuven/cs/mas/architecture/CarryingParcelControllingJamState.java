package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.agent.AGVAgent;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.PDPModel.ParcelState;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CarryingParcelControllingJamState extends CarryingParcelState {

	private int step = 0;
	private long timeOutCount = 0;
	private long timeStamp = 0;
	private Point nextWantedPoint;

	public CarryingParcelControllingJamState(AGVAgent agent, long timeStamp) {
		super(agent);
		this.nextWantedPoint = agent.getNextPointOnPath().get();
		this.timeStamp = timeStamp;
	}
	
	public CarryingParcelControllingJamState(AGVAgent agent, List<ReleaseBacklog> backLogs, long timeStamp) {
		super(agent, backLogs);
		this.nextWantedPoint = agent.getNextPointOnPath().get();
		this.timeStamp = timeStamp;
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
				this.afterMoveForward(timeLapse);
				return;
			}
		}

		if (this.timeOutOccurred()) {
			// resend move-aside
			this.sendMoveAside(this.getAgent().getName(), Arrays.asList(this.getAgent().getName()), this.getTimeStamp(),
					this.getAgent().getParcel().get().getWaitingSince(), this.getNextWantedPoint(), this.getStep());
		}
	}

	private void afterMoveForward(TimeLapse timeLapse) {
		this.setStep(this.getStep() + 1);
		this.setTimeStamp(timeLapse.getTime());

		this.setNextWantedPoint(this.getAgent().getNextPointOnPath().get());
		if (! this.getAgent().occupiedPointsOnPathWithinRange()) {
			// we are home free
			this.sendHomeFree();
			this.doStateTransition(Optional.of(new CarryingParcelNoJamState(this.getAgent(), this.getBackLogs(), false)));
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

	@Override
	protected void sendMoveAside(String requester, List<String> waitFor, long timeStamp, long parcelWaitingSince,
			Point wantPos, int step) {
		super.sendMoveAside(requester, waitFor, timeStamp, parcelWaitingSince, wantPos, step);
		this.setTimeOutCount(0);
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
			this.doStateTransition(Optional.of(new FollowGradientFieldState(this.getAgent(), this.getBackLogs())));
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

	private long getTimeStamp() {
		return this.timeStamp;
	}
	
	private void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	private Point getNextWantedPoint() {
		return this.nextWantedPoint;
	}

	private void setNextWantedPoint(Point point) {
		this.nextWantedPoint = point;
	}

	protected void processMoveAsideMessage(MoveAsideMessage msg) {
		boolean sawOwnName = false;
		if (msg.getRequester().equals(this.getAgent().getName())) {
			// monitor for possible deadlock
			sawOwnName = true;
		}
		if (msg.getPropagator().equals(this.getAgent().getName())) {
			return;
		}
		if (! (this.getAgent().getPosition().equals(msg.getWantPos())
				|| this.getAgent().getRoadModel().isOnConnectionTo(this.getAgent(), msg.getWantPos()))) {
			// requester does not want our point
			return;
		}
		if (sawOwnName) {
			// deadlock has occurred, restart protocol
			this.sendHomeFree();
			this.sendMoveAside(this.getAgent().getName(), Arrays.asList(this.getAgent().getName()), this.getTimeStamp(),
					this.getAgent().getParcel().get().getWaitingSince(), this.getNextWantedPoint(), this.getStep());
		}
		if (this.trafficPriorityFunction(msg.getRequester(), msg.getParcelWaitingSince())) {
			this.getAgent().getMessageBuilder().addField("reject")
			.addField("requester", msg.getRequester())
			.addField("propagator", msg.getPropagator())
			.addField("timestamp", Long.toString(msg.getTimeStamp()));
			this.getAgent().sendMessage(this.getAgent().getMessageBuilder().build());
		} else {
			this.getAgent().getMessageBuilder().addField("ack")
			.addField("requester", msg.getRequester())
			.addField("propagator", msg.getPropagator())
			.addField("timestamp", Long.toString(msg.getTimeStamp()));
			this.getAgent().sendMessage(this.getAgent().getMessageBuilder().build());
			// release all agents waiting on this agent
			this.sendHomeFree();
			this.doStateTransition(Optional.of(new CarryingParcelGetOutOfTheWayState(this.getAgent(), this.getBackLogs(), msg.getRequester(), msg.getPropagator(), msg.getWaitForList(), msg.getTimeStamp(), msg.getParcelWaitingSince(), msg.getStep(), msg.getAtPos())));
		}
	}

	protected void processRejectMessage(RejectMessage msg) {
		if (! msg.getRequester().equals(this.getAgent().getName())) {
			return;
		}
		if (! msg.getPropagator().equals(this.getAgent().getName())) {
			return;
		}
		if (msg.getTimeStamp() < this.getTimeStamp()) {
			return;
		}
		// reset time-out
		this.setTimeOutCount(0);
	}

	// TODO are ack messages necessary?
	protected void processAckMessage(AckMessage msg) {
		if (! msg.getRequester().equals(this.getAgent().getName())) {
			return;
		}
		if (! msg.getPropagator().equals(this.getAgent().getName())) {
			return;
		}
		if (msg.getTimeStamp() < this.getTimeStamp()) {
			return;
		}
		// reset time-out
		this.setTimeOutCount(0);
	}

	@Override
	protected void processReleaseMessage(ReleaseMessage msg) {
		super.processReleaseMessage(msg);
	}

	@Override
	protected void processHomeFreeMessage(HomeFreeMessage msg) {
		// TODO Auto-generated method stub
		
	}
}

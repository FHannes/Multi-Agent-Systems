package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.agent.AGVAgent;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.*;

public class CarryingParcelGetOutOfTheWayState extends CarryingParcelState {

	private String requester;
	private String propagator;
	private long parcelWaitTime;
	private int step;
	private Point nextWantedPoint;
	private Point propagatorPos;
	private Point propagatorWantPos;
	private boolean waitingOnOther = false;
	private boolean hasMoved = false;
	private long timeOutCount = 0;
	private long timeStamp = 0;
	private List<String> waitForList;

	public CarryingParcelGetOutOfTheWayState(AGVAgent agent, String requester, String propagator, List<String> waitForList, long timeStamp, long parcelWaitTime, int step, Point propagatorWantPos, Point propagatorPos) {
		super(agent);
		this.requester = requester;
		this.propagator = propagator;
		this.parcelWaitTime = parcelWaitTime;
		this.propagatorPos = propagatorPos;
		this.propagatorWantPos = propagatorWantPos;
		this.step = step;
		this.waitForList = waitForList;
		this.timeStamp = timeStamp;
		
		this.doMoveAside();
	}
	
	public CarryingParcelGetOutOfTheWayState(AGVAgent agent, List<ReleaseBacklog> backLogs, String requester, String propagator, List<String> waitForList, long timeStamp, long parcelWaitTime, int step, Point propagatorWantPos, Point propagatorPos) {
		super(agent, backLogs);
		this.requester = requester;
		this.propagator = propagator;
		this.parcelWaitTime = parcelWaitTime;
		this.propagatorPos = propagatorPos;
		this.propagatorWantPos = propagatorWantPos;
		this.step = step;
		this.waitForList = waitForList;
		this.timeStamp = timeStamp;
		
		this.doMoveAside();
	}

	@Override
	public void act(TimeLapse timeLapse) {
		
		boolean wasAlreadyWaiting = this.isWaitingOnOther();
		if (wasAlreadyWaiting) {
			this.setTimeOutCount(this.getTimeOutCount() + timeLapse.getTimeLeft());
		}
		
		if (this.hasMoved()) {
			return;
		}
		
		Set<Point> occupiedPoints = this.getAgent().getOccupiedPointsInVisualRange();
		if (! occupiedPoints.contains(this.getNextWantedPoint())) {
			// we can indeed move forward
			this.doMoveForward(this.getNextWantedPoint(), this.getAgent().getPosition().get(), timeLapse);
			if (this.getAgent().getPosition().get().equals(this.getNextWantedPoint())) {
				// move forward has succeeded
				this.setWaitingOnOther(false);
				if (this.getAgent().getPosition().get().equals(this.getPropagatorWantPos())) {
					this.doMoveAside();
				} else {
					this.setHasMoved(true);
				}
			}
		}
		
		// first of all, check if the parcel can be delivered
		if (this.getAgent().getPosition().equals(this.getAgent().getParcel().get().getDestination())) {
			this.getAgent().getPDPModel().deliver(this.getAgent(), this.getAgent().getParcel().get(), timeLapse);
			// if parcel is not in cargo anymore, delivery was successful
			if (! this.getAgent().getPDPModel().containerContains(this.getAgent(), this.getAgent().getParcel().get())) {
				// parcel has been delivered, so follow gradient field now
				this.getAgent().getParcel().get().notifyDelivered(timeLapse);
				this.sendRelease(this.getRequester(), this.getTimeStamp());
				this.doStateTransition(Optional.of(new FollowGradientFieldState(this.getAgent(), this.getBackLogs())));
			}
			return;
		}
		
		if (this.hasMoved()) {
			return;
		}
		
		this.setWaitingOnOther(true);
		if (this.timeOutOccurred() || !wasAlreadyWaiting) {
			// resend move-aside
			this.sendMoveAside(this.getRequester(), this.getWaitForListWithSelf(), this.getTimeStamp(),
					this.getParcelWaitTime(), this.getNextWantedPoint(), this.getStep());
			this.setTimeOutCount(0);
		}
	}

	@Override
	protected void handleExceptionDuringMove(TimeLapse timeLapse) {

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
	
	protected void processMoveAsideMessage(MoveAsideMessage msg) {
		boolean handleDeadlock = false;
		
		if (this.hasDeadlock(msg.getWaitForList())) {
			handleDeadlock = true;
		}
		if (! (this.getAgent().getPosition().get().equals(msg.getWantPos())
				|| this.getNextWantedPoint().equals(msg.getWantPos())
				|| this.getAgent().getRoadModel().occupiesPoint(this.getAgent(), msg.getWantPos())
				|| this.getAgent().getRoadModel().occupiesPointWithRespectTo(this.getAgent(), msg.getWantPos(), msg.getAtPos()))) {
			// propagator does not want our position
			return;
		}
		if (handleDeadlock) { // if there is a deadlock, it can now be handled properly
			this.sendRelease(this.getRequester(), this.getTimeStamp());
			this.doMoveAside(this.getNextWantedPoint()); // try again, this time trying to move to a different point
			// this will always work because only "get out of the way" agents at a junction will ever detect deadlock
			// since the controller will detect it first in the other case
			return;
		}
		if (msg.getRequester().equals(this.getRequester())) {
			// check if the step has increased, in which case the agent must once again move out of the way
			if ((msg.getStep() > this.getStep()) ||
					(this.hasMoved() && msg.getStep() == this.getStep())) { // must also move aside if the step remained the same
				// but an earlier move aside was not enough to solve the problem
				if (msg.getStep() == this.getStep() && msg.getTimeStamp() < this.getTimeStamp()) {
					// do not listen to messages with expired timestamps
					this.sendReject(msg.getRequester(), msg.getPropagator(), msg.getTimeStamp());
					return;
				}
				this.doMoveAside(msg.getAtPos());
				this.setPropagator(msg.getPropagator());
				this.setStep(msg.getStep());
				this.setTimeStamp(msg.getTimeStamp());
				this.setHasMoved(false);
			}
			this.setWaitForList(msg.getWaitForList());
			return;
		}
		if (AgentState.trafficPriorityFunction(this.getRequester(), msg.getRequester(), this.getParcelWaitTime(), msg.getParcelWaitingSince())) {
			this.sendReject(msg.getRequester(), msg.getPropagator(), msg.getTimeStamp());
		} else {
			// must now get out of the way for the new requester
			this.sendAck(msg.getRequester(), msg.getPropagator(), msg.getTimeStamp());
			this.setRequester(msg.getRequester());
			this.setPropagator(msg.getPropagator());
			this.setParcelWaitTime(msg.getParcelWaitingSince());
			this.setStep(msg.getStep());
			this.setPropagatorPos(msg.getAtPos());
			this.setPropagatorWantPos(msg.getWantPos());
			this.setWaitForList(msg.getWaitForList());
			this.setTimeOutCount(0);
			this.setTimeStamp(msg.getTimeStamp());
			this.setWaitingOnOther(false);
			this.sendRelease(this.getRequester(), this.getTimeStamp());
			this.doMoveAside(msg.getAtPos());
		}
	}
	

	protected void processRejectMessage(RejectMessage msg) {
		if (! msg.getRequester().equals(this.getRequester())) {
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

	protected void processAckMessage(AckMessage msg) {
		if (! msg.getRequester().equals(this.getRequester())) {
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
	
	protected void processReleaseMessage(ReleaseMessage msg) {
		super.processReleaseMessage(msg);
		if (! msg.getRequester().equals(this.getRequester())) {
			return;
		}
		if (! msg.getPropagator().equals(this.getPropagator())) {
			return;
		}
		if (msg.getTimeStamp() < this.getTimeStamp()) {
			return;
		}
		// the propagator has released this agent from coordinating the traffic jam
		this.sendRelease(this.getRequester(), this.getTimeStamp()); // propagate the release in order to release agents this agent is (indirectly) waiting on
		this.doStateTransition(Optional.of(new CarryingParcelNoJamState(this.getAgent(), this.getBackLogs(), true)));
	}
	
	protected void processHomeFreeMessage(HomeFreeMessage msg) {
		if (msg.getRequester().equals(this.getRequester())) {
			this.doStateTransition(Optional.of(new CarryingParcelNoJamState(this.getAgent(), this.getBackLogs(), true)));
		}
	}
	
	private void doMoveAside(Point... additionalForbidden) {
		Set<Point> mustExclude = new HashSet<Point>(Arrays.asList(this.getPropagatorPos()));
		mustExclude.addAll(Arrays.asList(additionalForbidden));
		Set<Point> tryAgain = new HashSet<Point>(Arrays.asList(this.getPropagatorWantPos()));
		Optional<Point> nextStep = agent.getRandomReachablePoint(mustExclude, tryAgain);
		if (nextStep.isPresent()) {
			this.setNextWantedPoint(nextStep.get());
		} else {
			nextStep = agent.getRandomNeighbourPoint(mustExclude, tryAgain);
			// we assume that every point has a neighbour
			this.setNextWantedPoint(nextStep.get());
		}
		this.setHasMoved(false);
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
	
	private Point getPropagatorPos() {
		return this.propagatorPos;
	}
	
	private void setPropagatorPos(Point propagatorPos) {
		this.propagatorPos = propagatorPos;
	}
	
	private Point getPropagatorWantPos() {
		return this.propagatorWantPos;
	}
	
	private void setPropagatorWantPos(Point propagatorWantPos) {
		this.propagatorWantPos = propagatorWantPos;
	}

	private boolean isWaitingOnOther() {
		return waitingOnOther;
	}

	private void setWaitingOnOther(boolean waitingOnOther) {
		this.waitingOnOther = waitingOnOther;
	}

	private boolean hasMoved() {
		return hasMoved;
	}

	private void setHasMoved(boolean hasMoved) {
		this.hasMoved = hasMoved;
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
	
	private long getTimeStamp() {
		return this.timeStamp;
	}
	
	private void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	private List<String> getWaitForList() {
		return this.waitForList;
	}
	
	private void setWaitForList(List<String> waitForList) {
		this.waitForList = waitForList;
	}
	
	private List<String> getWaitForListWithSelf() {
		List<String> toReturn = new LinkedList<>(this.getWaitForList());
		toReturn.add(this.getAgent().getName());
		return toReturn;
	}

	@Override
	protected void processPleaseConfirmMessage(PleaseConfirmMessage msg) {
		if (msg.getRequester().equals(this.getRequester())
				&& msg.getPropagator().equals(this.getAgent().getName())) {
			if (this.getTimeStamp() >= msg.getTimeStamp() && 
					msg.getConfirmPositions().contains(this.getNextWantedPoint())) {
				this.sendDoConfirm(this.getRequester(), this.getAgent().getName(), this.getTimeStamp(), msg.getConfirmPositions());
			} else {
				this.sendNotConfirm(this.getRequester(), this.getAgent().getName(), this.getTimeStamp(), msg.getConfirmPositions());
			}
		}
	}

	@Override
	protected void processDoConfirmMessage(DoConfirmMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processNotConfirmMessage(NotConfirmMessage msg) {
		// TODO Auto-generated method stub
		
	}
}

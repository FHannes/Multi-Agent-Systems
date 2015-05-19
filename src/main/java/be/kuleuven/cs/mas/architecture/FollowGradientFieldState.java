package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.agent.AGVAgent;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;

import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;

public class FollowGradientFieldState extends AgentState {

	private Multimap<String, Point> forbiddenPoints = LinkedHashMultimap.create();
	private Optional<String> requester = Optional.absent();
	private Optional<Point> nextRequestedPoint = Optional.absent();
	private Optional<Point> potentialRequestedPoint = Optional.absent();
	private Optional<Point> nextSelectedPoint = Optional.absent();
	private Map<String,List<String>> waitForMap = new HashMap<>();
	private long timeStamp = 0;
	private long parcelWaitingSince = 0;
	private int numWaitingForConfirm = 0;
	private int step = 0;

	public FollowGradientFieldState(AGVAgent agent) {
		super(agent);
		requester = Optional.absent();
		nextRequestedPoint = Optional.absent();
	}
	
	public FollowGradientFieldState(AGVAgent agent, List<ReleaseBacklog> backLogs) {
		super(agent, backLogs);
		requester = Optional.absent();
		nextRequestedPoint = Optional.absent();
	}

	@Override
	public void act(TimeLapse timeLapse) {
		// TODO move code related to following gradient field here, exclude points in forbiddenPoints from
		// possible points to move to

		if (getAgent().getPDPModel().getContents(getAgent()).isEmpty()) {
			Set<TimeAwareParcel> parcels = getAgent().getRoadModel().getObjectsAt(getAgent(), TimeAwareParcel.class);
			if (!parcels.isEmpty()) {
				TimeAwareParcel parcel = parcels.stream().findFirst().get();
				getAgent().getPDPModel().pickup(getAgent(), parcel, timeLapse);
			}
		}

		ImmutableSet<Parcel> parcels = getAgent().getPDPModel().getContents(getAgent());
		if (!parcels.isEmpty()) {
			TimeAwareParcel parcel = (TimeAwareParcel) parcels.stream().findFirst().get();
			parcel.notifyPickup();
			// expand backlogs
			List<ReleaseBacklog> backlogs = this.getBackLogs();
			if (this.getRequester().isPresent()) {
				List<String> toPropagateFor = new ArrayList<>(this.getForbiddenPoints().keySet());
				backlogs.add(new ReleaseBacklog(this.getRequester().get(), this.getTimeStamp(), toPropagateFor));
			}
			doStateTransition(Optional.of(new CarryingParcelNoJamState(getAgent(), backlogs, parcel)));
			return;
		}

		Set<Point> occupied = getAgent().getOccupiedPointsInVisualRange();

		if (! this.getAgent().getMostRecentPosition().equals(this.getAgent().getPosition().get())) {
			// agent has started moving
		}

		if (! this.getNextSelectedPoint().isPresent()) {
			this.followGradientField(occupied);
		}
		if (this.getNextSelectedPoint().isPresent()
				&& this.getNextRequestedPoint().isPresent()
				&& ! this.getNextSelectedPoint().get().equals(this.getNextRequestedPoint().get())
				&& ! occupied.contains(this.getNextSelectedPoint().get())
				&& ! this.getForbiddenPoints().values().contains(this.getNextSelectedPoint().get())) {
			// agent has committed to different point than requested, so release any agents waiting on this one
			this.sendRelease(this.getRequester().get(), this.getTimeStamp());
			this.setNextRequestedPoint(Optional.absent());
		}
//		if (this.getNextSelectedPoint().isPresent()
//				&& this.getNextRequestedPoint().isPresent()
//				&& this.getNextSelectedPoint().get().equals(this.getNextRequestedPoint().get())
//				&& occupied.contains(this.getNextSelectedPoint().get())) {
//			this.followGradientFieldTryRequested(occupied);
//		}
		if (this.getNextSelectedPoint().isPresent() && ! occupied.contains(this.getNextSelectedPoint().get())) {
			this.getAgent().getRoadModel().moveTo(this.getAgent(), this.getNextSelectedPoint().get(), timeLapse);
			if (this.getAgent().getMostRecentPosition().equals(this.getNextSelectedPoint().get())) {
				this.setNextSelectedPoint(Optional.absent());
			}
		}
	}

	private void followGradientField(Set<Point> occupied, Point... forbidden) {
		Queue<Point> targets = getAgent().getGradientModel().getGradientTargets(getAgent());
		java.util.Optional<Point> target = targets.stream().filter(t -> !forbiddenPoints.containsValue(t) &&
				!occupied.contains(t)).findFirst();
		if (!target.isPresent()) {
			if (this.getRequester().isPresent() && ! this.getNextRequestedPoint().isPresent()
					&& ! this.getPotentialRequestedPoint().isPresent()) {
				Set<Point> excludeSet = new HashSet<>(this.getForbiddenPoints().values());
				excludeSet.addAll(Arrays.asList(forbidden));
				this.setPotentialRequestedPoint(Optional.of(this.getAgent().getRandomNeighbourPoint(excludeSet).get()));
				this.setNumWaitingForConfirm(this.getForbiddenPoints().keySet().size());
				for (String propagator : this.getForbiddenPoints().keySet()) {
					this.sendPleaseConfirm(this.getRequester().get(), propagator, this.getTimeStamp(), this.getAgent().getMostRecentPosition());
				}
			}
		} else {
			this.setNextSelectedPoint(Optional.of(target.get()));
		}
	}
	
//	private void followGradientFieldTryRequested(Set<Point> occupied) {
//		Queue<Point> targets = getAgent().getGradientModel().getGradientTargets(getAgent());
//		java.util.Optional<Point> target = targets.stream().filter(t -> !forbiddenPoints.containsValue(t) &&
//				!occupied.contains(t)).findFirst();
//		if (!target.isPresent()) {
//			if (this.getRequester().isPresent() && this.getNextRequestedPoint().isPresent()) {
//				this.sendMoveAside(this.getRequester().get(), this.getWaitForList().get(), this.getTimeStamp(),
//						this.getParcelWaitingSince(), this.getNextRequestedPoint().get(), this.getStep());
//			}
//		} else {
//			this.setNextSelectedPoint(Optional.of(target.get()));
//		}
//	}

	protected void processMoveAsideMessage(MoveAsideMessage msg) {
		boolean handleDeadlock = false;
		if (this.getRequester().isPresent() && this.hasDeadlock(msg.getWaitForList()) && msg.getTimeStamp() >= this.getTimeStamp()) {
			handleDeadlock = true;
		}
		if (! handleDeadlock &&
				this.getRequester().isPresent() &&
				! msg.getRequester().equals(this.getRequester().get()) &&
				AgentState.trafficPriorityFunction(this.getRequester().get(), msg.getRequester(),
						this.getParcelWaitingSince(), msg.getParcelWaitingSince())) {
			// there is already a requester active and that one has higher priority than the requester currently
			// under consideration
			this.sendReject(msg.getRequester(), msg.getPropagator(), msg.getTimeStamp());
			return;
		}
		if (! (this.getAgent().getPosition().get().equals(msg.getWantPos())
				|| (this.getNextSelectedPoint().isPresent() &&
						this.getNextSelectedPoint().get().equals(msg.getWantPos()) &&
						this.getAgent().getRoadModel().isOnConnectionTo(this.getAgent(),
								this.getNextSelectedPoint().get())))) {
			// propagator does not want our position
			return;
		}
		if (handleDeadlock) {
			this.sendRelease(this.getRequester().get(), this.getTimeStamp());
			this.followGradientField(this.getAgent().getOccupiedPointsInVisualRange(), this.getNextRequestedPoint().get());
		}
		if (this.getRequester().isPresent() && this.getRequester().get().equals(msg.getRequester()) && msg.getStep() < this.getStep()) {
			return;
		} else if ((this.getRequester().isPresent() && this.getRequester().get().equals(msg.getRequester()) &&  msg.getStep() > this.getStep()) ||
				! this.getRequester().isPresent() || ! this.getRequester().get().equals(msg.getRequester())) {
			this.getForbiddenPoints().clear();
			this.getWaitForMap().clear();
		}
		if (! this.getRequester().isPresent() || ! this.getRequester().get().equals(msg.getRequester())) {
			if (this.getRequester().isPresent() && ! this.getRequester().get().equals(msg.getRequester())) {
				this.sendRelease(this.getRequester().get(), this.getTimeStamp());
			}
			this.setRequester(Optional.of(msg.getRequester()));
			this.setParcelWaitingSince(msg.getParcelWaitingSince());
		}

		this.setStep(msg.getStep());
		this.getWaitForMap().put(msg.getPropagator(), msg.getWaitForList());
		this.getForbiddenPoints().put(msg.getPropagator(), msg.getAtPos());
		this.getForbiddenPoints().put(msg.getPropagator(), msg.getWantPos());
		this.sendAck(msg.getRequester(), msg.getPropagator(), msg.getTimeStamp());
	}

	protected void processRejectMessage(RejectMessage msg) {
		if (! this.getRequester().isPresent() || ! msg.getRequester().equals(this.getRequester().get())) {
			return;
		}
		if (! msg.getPropagator().equals(this.getAgent().getName())) {
			return;
		}
		// TODO select new point to navigate to, but requestedPoint may not be selected (however, do not add to forbiddenPoints)
		if (this.getNextRequestedPoint().isPresent()) {
			this.followGradientField(this.getAgent().getOccupiedPointsInVisualRange(), this.getNextRequestedPoint().get());
		}
	}

	protected void processHomeFreeMessage(HomeFreeMessage msg) {
		if (! this.getRequester().isPresent() || ! msg.getRequester().equals(this.getRequester().get())) {
			return;
		}
		this.setRequester(Optional.absent());
		this.getForbiddenPoints().clear();
		this.getWaitForMap().clear();
		this.setParcelWaitingSince(0);
		this.setStep(0);
		this.setTimeStamp(0);
	}

	protected void processReleaseMessage(ReleaseMessage msg) {
		super.processReleaseMessage(msg);
		if (! this.getRequester().isPresent() || ! msg.getRequester().equals(this.getRequester().get())) {
			return;
		}
		if (msg.getTimeStamp() < this.getTimeStamp()) {
			return;
		}
		this.sendRelease(this.getRequester().get(), this.getTimeStamp()); // release all agents this agent (indirectly) caused to activate "get out of the way"
		this.getForbiddenPoints().removeAll(msg.getPropagator());
		if (this.getForbiddenPoints().isEmpty()) {
			this.setNextRequestedPoint(Optional.absent());
			this.setPotentialRequestedPoint(Optional.absent());
			this.setRequester(Optional.absent());
			this.setParcelWaitingSince(0);
			this.setStep(0);
			this.setTimeStamp(0);
			this.getWaitForMap().clear();
		}
	}

	@Override
	protected void doStateTransition(Optional<AgentState> nextState) {
		this.getAgent().setAgentState(nextState.get());
	}

	@Override
	public void uponSet() {
		if (this.getAgent().getParcel().isPresent()) {
			this.getAgent().unsetParcel();
		}
	}

	@Override
	public double getFieldStrength() {
		return getAgent().getFieldStrategy().calculateFieldStrength(0);
	}

	private Multimap<String, Point> getForbiddenPoints() {
		return this.forbiddenPoints;
	}

	private Optional<String> getRequester() {
		return this.requester;
	}

	private void setRequester(Optional<String> requester) {
		this.requester = requester;
	}

	private long getTimeStamp() {
		return this.timeStamp;
	}
	
	private void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	private long getParcelWaitingSince() {
		return this.parcelWaitingSince;
	}

	private void setParcelWaitingSince(long parcelWaitingSince) {
		this.parcelWaitingSince = parcelWaitingSince;
	}

	private int getNumWaitingForConfirm() {
		return numWaitingForConfirm;
	}

	private void setNumWaitingForConfirm(int numWaitingForConfirm) {
		this.numWaitingForConfirm = numWaitingForConfirm;
	}

	private int getStep() {
		return this.step;
	}

	private void setStep(int step) {
		this.step = step;
	}

	private Optional<Point> getNextRequestedPoint() {
		return this.nextRequestedPoint;
	}

	private void setNextRequestedPoint(Optional<Point> point) {
		this.nextRequestedPoint = point;
	}

	private Optional<Point> getPotentialRequestedPoint() {
		return potentialRequestedPoint;
	}

	private void setPotentialRequestedPoint(Optional<Point> potentialRequestedPoint) {
		this.potentialRequestedPoint = potentialRequestedPoint;
	}

	private Optional<Point> getNextSelectedPoint() {
		return nextSelectedPoint;
	}

	private void setNextSelectedPoint(Optional<Point> nextSelectedPoint) {
		this.nextSelectedPoint = nextSelectedPoint;
	}

	private Map<String,List<String>> getWaitForMap() {
		return this.waitForMap;
	}

	private List<String> getWaitForListWithSelf(String propagator) throws IllegalStateException {
		if (! this.getWaitForMap().containsKey(propagator)) {
			throw new IllegalStateException("should only be called if there is a key for the propagator");
		}
		List<String> toReturn = new LinkedList<>(this.getWaitForMap().get(propagator));
		toReturn.add(this.getAgent().getName());
		return toReturn;
	}

	private void setWaitForMap(Map<String,List<String>> waitForMap) {
		this.waitForMap = waitForMap;
	}
	
	@Override
	protected void processAckMessage(AckMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processPleaseConfirmMessage(PleaseConfirmMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void processDoConfirmMessage(DoConfirmMessage msg) {
		if (this.getPotentialRequestedPoint().isPresent() && this.getRequester().isPresent()) {
			if (msg.getRequester().equals(this.getRequester().get()) && this.getForbiddenPoints().keySet().contains(msg.getPropagator())
					&& this.getTimeStamp() >= msg.getTimeStamp() && this.getAgent().getMostRecentPosition().equals(msg.getWantPos())) {
				this.setNextRequestedPoint(this.getPotentialRequestedPoint());
				this.setPotentialRequestedPoint(Optional.absent());
				this.setNumWaitingForConfirm(0);
				this.sendMoveAside(this.getRequester().get(), this.getWaitForListWithSelf(msg.getPropagator()), this.getTimeStamp(),
						this.getParcelWaitingSince(), this.getNextRequestedPoint().get(), this.getStep());
			}
		}
	}

	@Override
	protected void processNotConfirmMessage(NotConfirmMessage msg) {
		if (this.getNumWaitingForConfirm() > 0) {
			if (msg.getRequester().equals(this.getRequester().get()) &&
					this.getForbiddenPoints().keySet().contains(msg.getPropagator())
					&& this.getTimeStamp() >= msg.getTimeStamp()
					&& this.getAgent().getMostRecentPosition().equals(msg.getWantPos())) {
				this.setNumWaitingForConfirm(this.getNumWaitingForConfirm() - 1);
				this.getForbiddenPoints().removeAll(msg.getPropagator());
				if (this.getNumWaitingForConfirm() == 0) {
					this.setRequester(Optional.absent());
					this.setNextRequestedPoint(Optional.absent());
					this.setPotentialRequestedPoint(Optional.absent());
					this.getForbiddenPoints().clear();
					this.getWaitForMap().clear();
					this.setParcelWaitingSince(0);
					this.setTimeStamp(0);
				}
			}
		}
	}
}

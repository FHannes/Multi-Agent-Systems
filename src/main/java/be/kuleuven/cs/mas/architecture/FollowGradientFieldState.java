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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import java.util.Set;

public class FollowGradientFieldState extends AgentState {

	private Multimap<String, Point> propagatorWantPositions = LinkedHashMultimap.create();
	private Multimap<String, Point> propagatorPositions = LinkedHashMultimap.create();
	private Optional<String> requester = Optional.absent();
	private Optional<Point> nextRequestedPoint = Optional.absent();
	private Optional<Point> potentialRequestedPoint = Optional.absent();
	private Optional<String> confirmedPropagator = Optional.absent();
	private Optional<Point> nextSelectedPoint = Optional.absent();
	private Map<String,List<String>> waitForMap = new HashMap<>();
	private long timeStamp = 0;
	private long timeOutCount = 0;
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
		
		boolean wasAlreadyWaiting = this.getNumWaitingForConfirm() > 0 || this.getNextRequestedPoint().isPresent();
		boolean hasMoved = false;
		
		Point initialPosition = this.getAgent().getPosition().get();
		
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
				List<String> toPropagateFor = new ArrayList<>(this.getPropagatorWantPositions().keySet());
				backlogs.add(new ReleaseBacklog(this.getRequester().get(), this.getTimeStamp(), toPropagateFor));
			}
			doStateTransition(Optional.of(new CarryingParcelNoJamState(getAgent(), backlogs, parcel)));
			return;
		}

		Set<Point> occupied = getAgent().getOccupiedPointsInVisualRange();

		if (! this.getNextSelectedPoint().isPresent() ||
				(occupied.contains(this.getNextSelectedPoint().get()) && ! this.getNextRequestedPoint().isPresent() && ! this.getPotentialRequestedPoint().isPresent())) {
			this.followGradientField(occupied);
		}
		if (this.getNextSelectedPoint().isPresent()
				&& this.getRequester().isPresent()
				&& this.getNextRequestedPoint().isPresent()
				&& ! this.getNextSelectedPoint().get().equals(this.getNextRequestedPoint().get())
				&& ! occupied.contains(this.getNextSelectedPoint().get())
				&& ! this.getPropagatorWantPositions().values().contains(this.getNextSelectedPoint().get())) {
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
			this.doMoveForward(this.getNextSelectedPoint().get(), this.getAgent().getPosition().get(), timeLapse);
			if (this.getAgent().getPosition().get().equals(this.getNextSelectedPoint().get())) {
				this.setNextSelectedPoint(Optional.absent());
				this.setConfirmedPropagator(Optional.absent());
				this.setNextRequestedPoint(Optional.absent());
				if (! initialPosition.equals(this.getAgent().getPosition().get())) {
					hasMoved = true;
				}
			}
		}
		
		if (wasAlreadyWaiting) {
			this.setTimeOutCount(this.getTimeOutCount() + (timeLapse.getEndTime() - timeLapse.getStartTime()));
		}
		if (this.timeOutOccurred() && ! hasMoved) {
			if (this.getNumWaitingForConfirm() > 0) {
				this.resendPleaseConfirm();
				this.setTimeOutCount(0);
			} else if (this.getNextRequestedPoint().isPresent() && this.getRequester().isPresent()) {
				if (this.getPropagatorPositions().containsValue(this.getNextRequestedPoint().get())) {
					this.followGradientField(occupied);
				} else {
					this.sendMoveAside(this.getRequester().get(), Arrays.asList(this.getAgent().getName()), this.getTimeStamp(),
							this.getParcelWaitingSince(), this.getNextRequestedPoint().get(), this.getStep());
				}
//				this.sendMoveAside(this.getRequester().get(), this.getWaitForListWithSelf(this.getConfirmedPropagator().get()), this.getTimeStamp(),
//						this.getParcelWaitingSince(), this.getNextRequestedPoint().get(), this.getStep());
				this.setTimeOutCount(0);
			} else if (this.getNextSelectedPoint().isPresent() && this.getRequester().isPresent()) {
				this.setPotentialRequestedPoint(this.getNextSelectedPoint());
				this.resendPleaseConfirm();
			}
		}
		
		if (! this.getRequester().isPresent() && this.getAllForbiddenPoints().isEmpty() && ! this.getNextRequestedPoint().isPresent()
				&& ! this.getPotentialRequestedPoint().isPresent()) {
			// possibly fixes bug due to home free messages that go missing
			this.sendHomeFree();
		}
	}

	@Override
	protected void handleExceptionDuringMove(TimeLapse timeLapse) {
		
	}

	private void followGradientField(Set<Point> occupied, Point... tryWithout) {
		Queue<Point> targets = getAgent().getGradientModel().getGradientTargets(getAgent());
		java.util.Optional<Point> target = targets.stream().filter(t -> ! this.getPropagatorPositions().containsValue(t) &&
				!occupied.contains(t)).findFirst();
		if (!target.isPresent()) {
			if (this.getRequester().isPresent() && ! this.getNextRequestedPoint().isPresent()
					&& ! this.getPotentialRequestedPoint().isPresent()) {
				Set<Point> excludeSet = new HashSet<>(this.getPropagatorPositions().values());
				Set<Point> tryAgain = new HashSet<>(this.getPropagatorWantPositions().values());
				tryAgain.addAll(Arrays.asList(tryWithout));
				this.setPotentialRequestedPoint(Optional.of(this.getAgent().getRandomNeighbourPoint(excludeSet, tryAgain).get()));
				resendPleaseConfirm();
			}
		} else {
			this.setNextSelectedPoint(Optional.of(target.get()));
		}
	}

	private void resendPleaseConfirm() {
		this.setNumWaitingForConfirm(this.getPropagatorWantPositions().keySet().size());
		for (String propagator : this.getPropagatorWantPositions().keySet()) {
			this.sendPleaseConfirm(this.getRequester().get(), propagator, this.getTimeStamp(), this.getAllOccupiedPoints());
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
		if (! (this.getAgent().getPosition().get().equals(msg.getWantPos())
				|| (this.getNextSelectedPoint().isPresent() && this.getNextSelectedPoint().get().equals(msg.getWantPos()))
				|| this.getAgent().getRoadModel().occupiesPoint(this.getAgent(), msg.getWantPos())
				|| this.getAgent().getRoadModel().occupiesPointWithRespectTo(this.getAgent(), msg.getWantPos(), msg.getAtPos()))) {
			// propagator does not want our position
			return;
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
		if (handleDeadlock) {
			this.sendRelease(this.getRequester().get(), this.getTimeStamp());
			if (this.getNextRequestedPoint().isPresent()) {
				this.followGradientField(this.getAgent().getOccupiedPointsInVisualRange(), this.getNextRequestedPoint().get());
			} else {
				this.followGradientField(this.getAgent().getOccupiedPointsInVisualRange());
			}
		}
		if (this.getRequester().isPresent() && this.getRequester().get().equals(msg.getRequester()) && msg.getStep() < this.getStep()) {
			return;
		} else if ((this.getRequester().isPresent() && this.getRequester().get().equals(msg.getRequester()) &&  msg.getStep() > this.getStep()) ||
				! this.getRequester().isPresent() || ! this.getRequester().get().equals(msg.getRequester())) {
			this.clearForbiddenPoints();
			this.getWaitForMap().remove(msg.getPropagator());
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
		this.getPropagatorPositions().put(msg.getPropagator(), msg.getAtPos());
		this.getPropagatorWantPositions().put(msg.getPropagator(), msg.getWantPos());
		this.setTimeStamp(msg.getTimeStamp());
		this.sendAck(msg.getRequester(), msg.getPropagator(), msg.getTimeStamp());
		if (this.getNextSelectedPoint().isPresent() && this.getPropagatorPositions().values().contains(this.getNextSelectedPoint().get())) {
			this.setNextSelectedPoint(Optional.absent());
		}
		if (this.getNextRequestedPoint().isPresent() && this.getPropagatorPositions().values().contains(this.getNextRequestedPoint().get())) {
			this.setNextRequestedPoint(Optional.absent());
		}
		if (this.getPotentialRequestedPoint().isPresent() && this.getPropagatorPositions().values().contains(this.getPotentialRequestedPoint().get())) {
			this.setPotentialRequestedPoint(Optional.absent());
		}
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
		this.setNextRequestedPoint(Optional.absent());
		this.setPotentialRequestedPoint(Optional.absent());
		this.setNumWaitingForConfirm(0);
		this.clearForbiddenPoints();
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
//		this.getPropagatorWantPositions().removeAll(msg.getPropagator());
			this.setNextRequestedPoint(Optional.absent());
			this.setPotentialRequestedPoint(Optional.absent());
			this.setNumWaitingForConfirm(0);
			this.setRequester(Optional.absent());
			this.setParcelWaitingSince(0);
			this.setStep(0);
			this.setTimeStamp(0);
			this.getWaitForMap().clear();
			this.clearForbiddenPoints();
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

	private Multimap<String, Point> getPropagatorWantPositions() {
		return this.propagatorWantPositions;
	}
	
	private Multimap<String, Point> getPropagatorPositions() {
		return this.propagatorPositions;
	}
	
	private Collection<Point> getAllForbiddenPoints() {
		Set<Point> toReturn = new HashSet<Point>();
		toReturn.addAll(this.getPropagatorWantPositions().values());
		toReturn.addAll(this.getPropagatorPositions().values());
		return toReturn;
	}
	
	private void clearForbiddenPoints() {
		this.getPropagatorWantPositions().clear();
		this.getPropagatorPositions().clear();
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
	
	private long getTimeOutCount() {
		return this.timeOutCount;
	}
	
	private void setTimeOutCount(long timeOutCount) {
		this.timeOutCount = Math.min(timeOutCount, AgentState.RESEND_TIMEOUT);
	}
	
	private boolean timeOutOccurred() {
		return this.getTimeOutCount() >= AgentState.RESEND_TIMEOUT;
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
	
	private Optional<String> getConfirmedPropagator() {
		return this.confirmedPropagator;
	}
	
	private void setConfirmedPropagator(Optional<String> propagator) {
		this.confirmedPropagator = propagator;
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
		if (this.getAgent().getName().equals(msg.getPropagator())) {
			if (! this.getRequester().isPresent() || ! this.getRequester().get().equals(msg.getRequester())) {
				this.sendNotConfirm(msg.getRequester(), msg.getPropagator(), msg.getTimeStamp(), msg.getConfirmPositions());
				return;
			}
		}
		if (this.getTimeStamp() >= msg.getTimeStamp() && this.getNextSelectedPoint().isPresent() &&
				msg.getConfirmPositions().contains(this.getNextSelectedPoint().get())) {
			this.sendDoConfirm(msg.getRequester(), msg.getPropagator(), msg.getTimeStamp(), msg.getConfirmPositions());
		}
	}
	
	private Set<Point> getAllOccupiedPoints() {
		Set<Point> toReturn = new HashSet<>(this.getAgent().getRoadModel().getOccupiedPoints(this.getAgent()));
		if (! this.getAgent().getRoadModel().getGraph().containsNode(this.getAgent().getPosition().get())) {
			toReturn.add(this.getAgent().getRoadModel().getConnection(this.getAgent()).get().to());
		}
		return toReturn;
	}
	
	private boolean occupiedPointsContainsOneOf(Collection<Point> points) {
		Set<Point> occupied = this.getAllOccupiedPoints();
		for (Point ele : points) {
			if (occupied.contains(ele)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void processDoConfirmMessage(DoConfirmMessage msg) {
		if (this.getPotentialRequestedPoint().isPresent() && this.getRequester().isPresent()) {
			if (msg.getRequester().equals(this.getRequester().get()) &&
					this.getPropagatorWantPositions().keySet().contains(msg.getPropagator())
					&& this.getTimeStamp() >= msg.getTimeStamp() &&
					this.occupiedPointsContainsOneOf(msg.getConfirmPositions())) {
				this.setNextRequestedPoint(this.getPotentialRequestedPoint());
				this.setPotentialRequestedPoint(Optional.absent());
				this.setConfirmedPropagator(Optional.of(msg.getPropagator()));
				this.setNumWaitingForConfirm(0);
				this.sendMoveAside(this.getRequester().get(), this.getWaitForListWithSelf(msg.getPropagator()), this.getTimeStamp(),
						this.getParcelWaitingSince(), this.getNextRequestedPoint().get(), this.getStep());
				this.setTimeOutCount(0);
			}
		}
	}

	@Override
	protected void processNotConfirmMessage(NotConfirmMessage msg) {
		if (this.getNumWaitingForConfirm() > 0) {
			if (msg.getRequester().equals(this.getRequester().get()) &&
					this.getPropagatorWantPositions().keySet().contains(msg.getPropagator())
					&& this.getTimeStamp() >= msg.getTimeStamp()
					&& this.getAgent().getRoadModel().occupiesOneOfPoints(this.getAgent(), msg.getConfirmPositions())) {
				this.setNumWaitingForConfirm(this.getNumWaitingForConfirm() - 1);
				this.getPropagatorWantPositions().removeAll(msg.getPropagator());
				if (this.getNumWaitingForConfirm() == 0) {
					this.setRequester(Optional.absent());
					this.setNextRequestedPoint(Optional.absent());
					this.setPotentialRequestedPoint(Optional.absent());
					this.clearForbiddenPoints();
					this.getWaitForMap().remove(msg.getPropagator());
					this.setParcelWaitingSince(0);
					this.setTimeStamp(0);
				}
			}
		}
	}
}

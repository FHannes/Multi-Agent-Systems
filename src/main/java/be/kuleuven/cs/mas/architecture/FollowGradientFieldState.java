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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class FollowGradientFieldState extends AgentState {

	private Multimap<String, Point> forbiddenPoints = LinkedHashMultimap.create();
	private Optional<String> requester;
	private Optional<Point> nextRequestedPoint;
	private Optional<Point> nextSelectedPoint;
	private Optional<List<String>> waitForList;
	private boolean hasMoved = false;
	private long timeStamp = 0;
	private long parcelWaitingSince = 0;
	private int step = 0;

	public FollowGradientFieldState(AGVAgent agent) {
		super(agent);
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
			doStateTransition(Optional.of(new CarryingParcelNoJamState(getAgent(), parcel)));
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
				&& ! this.getNextSelectedPoint().get().equals(this.getNextRequestedPoint())
				&& ! occupied.contains(this.getNextSelectedPoint().get())
				&& ! this.getForbiddenPoints().values().contains(this.getNextSelectedPoint())) {
			this.sendRelease(this.getTimeStamp());
			this.setNextRequestedPoint(Optional.absent());
			this.setHasMoved(true);
		}
		if (this.getNextSelectedPoint().isPresent()
				&& this.getNextRequestedPoint().isPresent()
				&& this.getNextSelectedPoint().get().equals(this.getNextRequestedPoint().get())
				&& occupied.contains(this.getNextSelectedPoint().get())) {
			this.followGradientFieldTryRequested(occupied);
		}
		if (this.getNextSelectedPoint().isPresent() && ! occupied.contains(this.getNextSelectedPoint())) {
			this.getAgent().getRoadModel().moveTo(this.getAgent(), this.getNextSelectedPoint().get(), timeLapse);
			if (this.getAgent().getPosition().get().equals(this.getNextSelectedPoint())) {
				if (this.getNextRequestedPoint().isPresent() &&
						this.getAgent().getPosition().get().equals(this.getNextRequestedPoint().get())) {
					this.setHasMoved(true);
				}
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
					&& !this.hasMoved()) {
				Set<Point> excludeSet = new HashSet<Point>(this.getForbiddenPoints().values());
				excludeSet.addAll(Arrays.asList(forbidden));
				this.setNextRequestedPoint((this.getAgent().getRandomNeighbourPoint(excludeSet)));
				this.sendMoveAside();
			}
		} else {
			this.setNextSelectedPoint(Optional.of(target.get()));
		}
	}
	
	private void followGradientFieldTryRequested(Set<Point> occupied) {
		Queue<Point> targets = getAgent().getGradientModel().getGradientTargets(getAgent());
		java.util.Optional<Point> target = targets.stream().filter(t -> !forbiddenPoints.containsValue(t) &&
				!occupied.contains(t)).findFirst();
		if (!target.isPresent()) {
			if (this.getRequester().isPresent() && this.getNextRequestedPoint().isPresent()
					&& !this.hasMoved()) {
				this.sendMoveAside();
			}
		} else {
			this.setNextSelectedPoint(Optional.of(target.get()));
		}
	}

	protected void processMoveAsideMessage(MoveAsideMessage msg) {
		boolean handleDeadlock = false;
		if (this.hasDeadlock(msg.getWaitForList()) && msg.getTimeStamp() >= this.getTimeStamp()) {
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
		if (! (this.getAgent().getPosition().equals(msg.getWantPos())
				|| (this.getNextSelectedPoint().isPresent() &&
						this.getNextSelectedPoint().get().equals(msg.getWantPos()) &&
						this.getAgent().getRoadModel().isOnConnectionTo(this.getAgent(),
								this.getNextSelectedPoint().get())))) {
			// propagator does not want our position
			return;
		}
		if (handleDeadlock) {
			this.sendRelease(this.getTimeStamp());
			this.followGradientField(this.getAgent().getOccupiedPointsInVisualRange(), this.getNextRequestedPoint().get());
		}
		if (this.getRequester().isPresent() && this.getRequester().get().equals(msg.getRequester()) && msg.getStep() < this.getStep()) {
			return;
		} else if ((this.getRequester().isPresent() && this.getRequester().get().equals(msg.getRequester()) &&  msg.getStep() > this.getStep()) ||
				! this.getRequester().isPresent() || ! this.getRequester().get().equals(msg.getRequester())) {
			this.getForbiddenPoints().clear();
			this.setHasMoved(false);
		}
		if (! this.getRequester().isPresent() || ! this.getRequester().get().equals(msg.getRequester())) {
			if (this.getRequester().isPresent() && ! this.getRequester().get().equals(msg.getRequester())) {
				this.sendRelease(this.getTimeStamp());
			}
			this.setRequester(Optional.of(msg.getRequester()));
			this.setParcelWaitingSince(msg.getParcelWaitingSince());
			this.setHasMoved(false);
		}

		this.setStep(msg.getStep());
		this.setWaitForList(Optional.of(msg.getWaitForList()));
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
		this.setWaitForList(Optional.absent());
		this.setParcelWaitingSince(0);
		this.setStep(0);
		this.setTimeStamp(0);
	}

	protected void processReleaseMessage(ReleaseMessage msg) {
		if (! this.getRequester().isPresent() || ! msg.getRequester().equals(this.getRequester().get())) {
			return;
		}
		if (msg.getTimeStamp() < this.getTimeStamp()) {
			return;
		}
		this.sendRelease(this.getTimeStamp()); // release all agents this agent (indirectly) caused to activate "get out of the way"
		this.getForbiddenPoints().removeAll(msg.getPropagator());
		this.setRequester(Optional.absent());
		this.setParcelWaitingSince(0);
		this.setStep(0);
		this.setTimeStamp(0);
		this.setWaitForList(Optional.absent());
	}

	private void sendMoveAside() {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("move-aside")
				.addField("requester", this.getRequester().get())
				.addField("propagator", this.getAgent().getName())
				.addField("wait-for", toWaitForString(this.getWaitForListWithSelf()))
				.addField("timestamp", Long.toString(this.getTimeStamp()))
				.addField("parcel-waiting-since", Long.toString(this.getParcelWaitingSince()))
				.addField("want-pos", this.getNextRequestedPoint().get().toString())
				.addField("at-pos", this.getAgent().getMostRecentPosition().toString())
				.addField("step", Integer.toString(this.getStep()))
				.build());
	}

	private void sendAck(String requester, String propagator, long timeStamp) {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("ack")
				.addField("requester", requester)
				.addField("propagator", propagator)
				.addField("timestamp", Long.toString(timeStamp))
				.build());
	}

	private void sendReject(String requester, String propagator, long timeStamp) {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("reject")
				.addField("requester", requester)
				.addField("propagator", propagator)
				.addField("timestamp", Long.toString(timeStamp))
				.build());
	}

	private void sendRelease(long timeStamp) {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("release")
				.addField("requester", this.getRequester().get())
				.addField("propagator", this.getAgent().getName())
				.addField("timestamp", Long.toString(this.getTimeStamp()))
				.build());
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
		// No repulsion field emitted when the agent is not carrying a parcel
		return 0;
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

	private Optional<Point> getNextSelectedPoint() {
		return nextSelectedPoint;
	}

	private void setNextSelectedPoint(Optional<Point> nextSelectedPoint) {
		this.nextSelectedPoint = nextSelectedPoint;
	}

	private Optional<List<String>> getWaitForList() {
		return this.waitForList;
	}

	private List<String> getWaitForListWithSelf() throws IllegalStateException {
		if (! this.getWaitForList().isPresent()) {
			throw new IllegalStateException("should only be called if there is a wait-for list");
		}
		List<String> toReturn = new LinkedList<>(this.getWaitForList().get());
		toReturn.add(this.getAgent().getName());
		return toReturn;
	}

	private void setWaitForList(Optional<List<String>> waitForList) {
		this.waitForList = waitForList;
	}

	private boolean hasMoved() {
		return hasMoved;
	}

	private void setHasMoved(boolean hasMoved) {
		this.hasMoved = hasMoved;
	}

	@Override
	protected void processAckMessage(AckMessage msg) {
		// TODO Auto-generated method stub
		
	}
}

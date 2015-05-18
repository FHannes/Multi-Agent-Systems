package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.AGVAgent;
import be.kuleuven.cs.mas.message.AgentMessage;
import be.kuleuven.cs.mas.message.Field;
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
				&& ! occupied.contains(this.getNextSelectedPoint().get())) {
			this.sendRelease();
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

	@Override
	public void processMessage(AgentMessage msg) {
		switch(msg.getContents().get(0).getName()) {
		case "move-aside": this.processMoveAsideMessage(msg);
		break;
		case "ack": this.processAckMessage(msg);
		break;
		case "reject": this.processRejectMessage(msg);
		break;
		case "home-free": this.processHomeFreeMessage(msg);
		break;
		case "release": this.processReleaseMessage(msg);
		break;
		default: return;
		}
	}

	private void processMoveAsideMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		boolean handleDeadlock = false;

		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("propagator")) {
			return;
		}
		String propagator = contents.get(i++).getValue();
		if (! contents.get(i).getName().equals("wait-for")) {
			return;
		}
		List<String> waitForList = toWaitForList(contents.get(i++).getValue());
		if (this.hasDeadlock(waitForList)) {
			handleDeadlock = true;
		}
		if (! contents.get(i).getName().equals("parcel-waiting-since")) {
			return;
		}
		long parcelWaitingSince = Long.parseLong(contents.get(i++).getValue());
		if (! handleDeadlock &&
				this.getRequester().isPresent() &&
				! requester.equals(this.getRequester().get()) &&
				AgentState.trafficPriorityFunction(this.getRequester().get(), requester,
						this.getParcelWaitingSince(), parcelWaitingSince)) {
			// there is already a requester active and that one has higher priority than the requester currently
			// under consideration
			this.sendReject(requester, propagator);
			return;
		}
		if (! contents.get(i).getName().equals("want-pos")) {
			return;
		}
		Point requestedPoint = Point.parsePoint(contents.get(i++).getValue());
		if (! (this.getAgent().getPosition().equals(requestedPoint)
				|| (this.getNextRequestedPoint().isPresent() && this.getNextRequestedPoint().get().equals(requestedPoint)))) {
			return;
		}
		if (! contents.get(i).getName().equals("at-pos")) {
			return;
		}
		Point propagatorPos = Point.parsePoint(contents.get(i++).getValue());
		if (handleDeadlock) {
			this.sendRelease();
			this.followGradientField(this.getAgent().getOccupiedPointsInVisualRange(), this.getNextRequestedPoint().get());
		}
		if (! contents.get(i).getName().equals("step")) {
			// invalid message, so ignore
			return;
		}
		int step = Integer.parseInt(contents.get(i).getValue());
		if (this.getRequester().isPresent() && this.getRequester().get().equals(requester) && step < this.getStep()) {
			return;
		} else if ((this.getRequester().isPresent() && this.getRequester().get().equals(requester) &&  step > this.getStep()) ||
				! this.getRequester().isPresent() || ! this.getRequester().get().equals(requester)) {
			this.getForbiddenPoints().clear();
			this.setHasMoved(false);
		}
		if (! this.getRequester().isPresent() || ! this.getRequester().get().equals(requester)) {
			if (this.getRequester().isPresent() && ! this.getRequester().get().equals(requester)) {
				this.sendRelease();
			}
			this.setRequester(Optional.of(requester));
			this.setParcelWaitingSince(parcelWaitingSince);
			this.setHasMoved(false);
		}

		this.setStep(step);
		this.setWaitForList(Optional.of(waitForList));
		this.getForbiddenPoints().put(propagator, propagatorPos);
		this.getForbiddenPoints().put(propagator, requestedPoint);
		this.sendAck(requester, propagator);
	}

	private void processAckMessage(AgentMessage msg) {

	}

	private void processRejectMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! this.getRequester().isPresent() || ! requester.equals(this.getRequester().get())) {
			return;
		}
		if (! contents.get(i).getName().equals("propagator")) {
			return;
		}
		String propagator = contents.get(i++).getValue();
		if (! propagator.equals(this.getAgent().getName())) {
			return;
		}
		// TODO select new point to navigate to, but requestedPoint may not be selected (however, do not add to forbiddenPoints)
		if (this.getNextRequestedPoint().isPresent()) {
			this.followGradientField(this.getAgent().getOccupiedPointsInVisualRange(), this.getNextRequestedPoint().get());
		}
	}

	private void processHomeFreeMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! this.getRequester().isPresent() || ! requester.equals(this.getRequester().get())) {
			return;
		}
		this.setRequester(Optional.absent());
		this.getForbiddenPoints().clear();
		this.setWaitForList(Optional.absent());
		this.setParcelWaitingSince(0);
		this.setStep(0);
	}

	private void processReleaseMessage(AgentMessage msg) {
		int i = 1;
		List<Field> contents = msg.getContents();
		if (! contents.get(i).getName().equals("requester")) {
			return;
		}
		String requester = contents.get(i++).getValue();
		if (! this.getRequester().isPresent() || ! requester.equals(this.getRequester())) {
			return;
		}
		if (! contents.get(i).getName().equals("propagator")) {
			return;
		}
		String propagator = contents.get(i++).getValue();
		this.sendRelease(); // release all agents this agent (indirectly) caused to activate "get out of the way"
		this.getForbiddenPoints().removeAll(propagator);
		this.setRequester(Optional.absent());
		this.setParcelWaitingSince(0);
		this.setStep(0);
		this.setWaitForList(Optional.absent());
	}

	private void sendMoveAside() {
		this.getAgent().sendMessage(this.getAgent().getMessageBuilder().addField("move-aside")
				.addField("requester", this.getRequester().get())
				.addField("propagator", this.getAgent().getName())
				.addField("wait-for", toWaitForString(this.getWaitForListWithSelf()))
				.addField("parcel-waiting-since", Long.toString(this.getParcelWaitingSince()))
				.addField("want-pos", this.getNextRequestedPoint().get().toString())
				.addField("at-pos", this.getAgent().getMostRecentPosition().toString())
				.addField("step", Integer.toString(this.getStep()))
				.build());
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
				.addField("requester", this.getRequester().get())
				.addField("propagator", this.getAgent().getName())
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
}

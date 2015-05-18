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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class FollowGradientFieldState extends AgentState {

	private Multimap<String, Point> forbiddenPoints = LinkedHashMultimap.create();
	private Optional<String> requester;
	private Optional<Point> nextSelectedPoint;
	private Optional<List<String>> waitForList;
	private long parcelWaitingSince = 0;
	private int step = 0;
	
	public FollowGradientFieldState(AGVAgent agent) {
		super(agent);
		requester = Optional.absent();
		nextSelectedPoint = Optional.absent();
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
		Queue<Point> targets = getAgent().getGradientModel().getGradientTargets(getAgent());
		while (!targets.isEmpty()) {
			Point target = targets.peek();
			if (!forbiddenPoints.containsValue(target) && !occupied.contains(target)) {
				break;
			}
			targets.poll();
		}
		Point target = targets.poll();
		if (target == null) {

		} else {

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
				|| (this.getNextSelectedPoint().isPresent() && this.getNextSelectedPoint().get().equals(requestedPoint)))) {
			return;
		}
		if (! contents.get(i).getName().equals("at-pos")) {
			return;
		}
		Point propagatorPos = Point.parsePoint(contents.get(i++).getValue());
		if (handleDeadlock) {
			this.sendRelease();
			// TODO select new point different from the propagator's position and the previously selected point
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
		}
		if (! this.getRequester().isPresent() || ! this.getRequester().get().equals(requester)) {
			if (this.getRequester().isPresent() && ! this.getRequester().get().equals(requester)) {
				this.sendRelease();
			}
			this.setRequester(Optional.of(requester));
			this.setParcelWaitingSince(parcelWaitingSince);
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
		// TODO select new point to navigate to, but requestedPoint may not be selected (however, do not add to forbiddenPoints)
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
				.addField("want-pos", this.getNextSelectedPoint().get().toString())
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
	
	private Optional<Point> getNextSelectedPoint() {
		return this.nextSelectedPoint;
	}
	
	private void setNextSelectedPoint(Optional<Point> point) {
		this.nextSelectedPoint = point;
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
}

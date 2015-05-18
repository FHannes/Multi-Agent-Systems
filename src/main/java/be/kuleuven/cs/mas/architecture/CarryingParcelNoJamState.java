package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.agent.AGVAgent;
import be.kuleuven.cs.mas.message.AgentMessage;
import be.kuleuven.cs.mas.message.Field;
import be.kuleuven.cs.mas.parcel.TimeAwareParcel;
import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;

import java.util.List;
import java.util.Set;

public class CarryingParcelNoJamState extends CarryingParcelState {

	public CarryingParcelNoJamState(AGVAgent agent, TimeAwareParcel parcel) {
		super(agent);
		agent.setParcel(parcel);
	}
	
	public CarryingParcelNoJamState(AGVAgent agent, boolean replanRoute) {
		super(agent);
		assert(agent.getParcel().isPresent());
		if (replanRoute) {
			agent.replanRoute();
		}
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

		// if not, follow the planned path
		Set<Point> occupied = this.getAgent().getOccupiedPointsInVisualRange();
		if (occupied.contains(this.getAgent().getNextPointOnPath())) {
			// we have a traffic jam, so begin protocol
			this.getAgent().getMessageBuilder().addField("move-aside")
			.addField("requester", this.getAgent().getName())
			.addField("propagator", this.getAgent().getName())
			.addField("parcel-waiting-since", Long.toString(this.getAgent().getParcel().get().getWaitingSince()))
			.addField("want-pos", this.getAgent().getNextPointOnPath().toString())
			.addField("at-pos", this.getAgent().getMostRecentPosition().toString())
			.addField("step", Integer.toString(0));
			this.getAgent().sendMessage(this.getAgent().getMessageBuilder().build());
			this.doStateTransition(Optional.of(new CarryingParcelControllingJamState(this.getAgent())));
		} else {
			// otherwise, move forward
			this.getAgent().followPath(timeLapse);
		}
	}

	@Override
	public void processMessage(AgentMessage msg) {
		int i = 0;
		List<Field> contents = msg.getContents();

		// ignore all except move-aside requests
		if (contents.get(i).getName().equals("move-aside")) {
			i++;
			if (! contents.get(i).getName().equals("requester")) {
				// invalid message, so ignore
				return;
			}
			String requester = contents.get(i++).getValue();
			if (requester.equals(this.getAgent().getName())) {
				// somehow received own move-aside request, so ignore
				return;
			}
			if (! contents.get(i).getName().equals("propagator")) {
				// invalid message, so ignore
				return;
			}
			String propagator = contents.get(i++).getValue();
			if (propagator.equals(this.getAgent().getName())) {
				// somehow received own propagated move-aside request, so ignore
				return;
			}
			if (! contents.get(i).getName().equals("wait-for")) {
				// invalid message, so ignore
				return;
			}
			List<String> waitForList = toWaitForList(contents.get(i++).getValue());
			if (! contents.get(i).getName().equals("parcel-waiting-since")) {
				// invalid message, so ignore
				return;
			}
			long parcelWaitTime = Long.parseLong(contents.get(i++).getValue());

			if (! contents.get(i).getName().equals("want-pos")) {
				// invalid message, so ignore
				return;
			}

			Point requestedPoint = Point.parsePoint(contents.get(i++).getValue());
			if (! (this.getAgent().getPosition().equals(requestedPoint)
					|| this.getAgent().getNextPointOnPath().equals(requestedPoint))) {
				// requester does not want this agent's position, so ignore
				return;
			}
			if (! contents.get(i).getName().equals("at-pos")) {
				// invalid message, so ignore
				return;
			}
			Point propagatorPos = Point.parsePoint(contents.get(i++).getValue());
			if (! contents.get(i).getName().equals("step")) {
				// invalid message, so ignore
				return;
			}

			if (this.trafficPriorityFunction(requester, parcelWaitTime)) {
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
				this.doStateTransition(Optional.of(new CarryingParcelGetOutOfTheWayState(this.getAgent(), requester, propagator, waitForList, parcelWaitTime, step, propagatorPos)));
			}
		}
	}

	@Override
	protected void doStateTransition(Optional<AgentState> nextState) {
		this.getAgent().setAgentState(nextState.get());
	}

	public void uponSet() {

	}

}

package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.AGVAgent;
import be.kuleuven.cs.mas.message.AgentMessage;
import com.github.rinde.rinsim.core.TimeLapse;
import com.google.common.base.Optional;

import java.util.regex.Matcher;

public abstract class CarryingParcelState extends AgentState {

	public CarryingParcelState(AGVAgent agent) {
		super(agent);
	}

	@Override
	public abstract void act(TimeLapse timeLapse);

	@Override
	public abstract void processMessage(AgentMessage msg);

	@Override
	public abstract void uponSet();

	@Override
	protected abstract void doStateTransition(Optional<AgentState> nextState);

	protected boolean trafficPriorityFunction(String requesterName, long parcelWaitTime)
			throws IllegalArgumentException, IllegalStateException {
		int compare = Long.compare(this.getAgent().getParcel().get().getWaitingSince(), parcelWaitTime);

		if (compare < 0) {
			return false;
		} else if (compare > 0) {
			return true;
		} else {
			Matcher reqMatcher = AgentState.NUM_PATTERN.matcher(requesterName);
			Matcher ownMatcher = NUM_PATTERN.matcher(this.getAgent().getName());

			if (! reqMatcher.find()) {
				throw new IllegalArgumentException("requester does not conform to agent name conventions");
			}
			if (! ownMatcher.find()) {
				throw new IllegalStateException("own name does not conform to agent name conventions");
			}

			// equality can never occur since agents cannot have the same number
			return Integer.parseInt(ownMatcher.group()) < Integer.parseInt(reqMatcher.group());
		}

	}
}

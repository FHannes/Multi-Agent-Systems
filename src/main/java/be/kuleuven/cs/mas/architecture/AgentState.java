package be.kuleuven.cs.mas.architecture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.rinde.rinsim.core.TimeLapse;
import com.google.common.base.Optional;

import be.kuleuven.cs.mas.message.AgentMessage;
import be.kuleuven.cs.mas.AGVAgent;

/**
 * Interface representing the state of an agent. The state determines the behaviour of the agent when told to act
 * or when receiving a message
 * 
 * @author Thomas
 *
 */
public abstract class AgentState {
	
	public static final long RESEND_TIMEOUT = 5000;
	protected static final Pattern NUM_PATTERN = Pattern.compile("\\d+");
	
	protected AGVAgent agent;
	
	public AgentState(AGVAgent agent) {
		this.agent = agent;
	}
	
	/**
	 * Performs one or more actions appropriate to the state
	 */
	public abstract void act(TimeLapse timeLapse);
	
	/**
	 * Processes the given message in a manner appropriate to the state
	 */
	public abstract void processMessage(AgentMessage msg);
	
	protected AGVAgent getAgent() {
		return this.agent;
	}
	
	protected void sendMessage(AgentMessage message) {
		this.getAgent().sendMessage(message);
	}
	
	public abstract void uponSet();
	
	/**
	 * Determine what the next state of the agent should be, either from internal information
	 * or the given recommended next state
	 */
	protected abstract void doStateTransition(Optional<AgentState> nextState);
	
	protected static boolean trafficPriorityFunction(String oneReq, String otherReq, long oneWaitTime, long otherWaitTime) {
		int compare = Long.compare(oneWaitTime, otherWaitTime);

		if (compare < 0) {
			return false;
		} else if (compare > 0) {
			return true;
		} else {
			Matcher oneReqMatcher = AgentState.NUM_PATTERN.matcher(oneReq);
			Matcher otherMatcher = NUM_PATTERN.matcher(otherReq);

			if (! oneReqMatcher.find()) {
				throw new IllegalArgumentException("oneReq does not conform to agent name conventions");
			}
			if (! otherMatcher.find()) {
				throw new IllegalStateException("otherReq does not conform to agent name conventions");
			}

			// equality can never occur since agents cannot have the same number
			return Integer.parseInt(otherMatcher.group()) < Integer.parseInt(oneReqMatcher.group());
		}

	}
}

package be.kuleuven.cs.mas.architecture;

import be.kuleuven.cs.mas.AGVAgent;
import be.kuleuven.cs.mas.message.AgentMessage;
import com.github.rinde.rinsim.core.TimeLapse;
import com.google.common.base.Optional;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	protected static final String DEADLOCK_SEP = ",";
	
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
			return true;
		} else if (compare > 0) {
			return false;
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
	
	protected static List<String> toWaitForList(String waitForString) {
		return new LinkedList<>(Arrays.asList(waitForString.split(DEADLOCK_SEP)));
	}
	
	protected static String toWaitForString(List<String> waitForList) {
		StringBuilder toReturn = new StringBuilder();
		for (String ele : waitForList) {
			toReturn.append(ele);
			toReturn.append(DEADLOCK_SEP);
		}
		return toReturn.toString();
	}
	
	protected boolean hasDeadlock(List<String> waitForList) {
		return waitForList.contains(this.getAgent().getName());
	}

	/**
	 * Returns the strength of the agent's influence on the gradient field. This value has to be positive as the agent
	 * emits a repulsion field.
	 */
	public abstract double getFieldStrength();

}

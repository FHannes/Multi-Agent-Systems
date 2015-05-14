package be.kuleuven.cs.mas.message;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
public class MessageContentsTest {
	@Rule
	ExpectedException exception = ExpectedException.none();
	
	String testMessage = "name" + AgentMessage.NAME_VALUE_SEP + "agent01" + AgentMessage.FIELD_SEP + "waiting-time" + AgentMessage.NAME_VALUE_SEP + "50s" + AgentMessage.FIELD_SEP + "hold" + AgentMessage.FIELD_SEP;
	AgentMessage msgContents;
	
	@Before
	public void setUp() throws Exception {
		msgContents = new AgentMessage(testMessage);
	}

	@Test
	public void constructorNullMessageTest() {
		exception.expect(IllegalArgumentException.class);
		new AgentMessage(null);
	}
	
	@Test
	public void constructorValidMessageTest() throws Exception {
		AgentMessage message = new AgentMessage(testMessage);
		assertEquals(testMessage, Whitebox.invokeMethod(message, "getMessage"));
	}
	
	@Test
	public void getContentsTest() {
		List<Field> fields = msgContents.getContents();
		assertEquals(3, fields.size());
		assertEquals("name", fields.get(0).getName());
		assertEquals("agent01", fields.get(0).getValue());
		assertEquals("waiting-time", fields.get(1).getName());
		assertEquals("50s", fields.get(1).getValue());
		assertFalse(fields.get(2).hasValue());
		assertEquals("hold", fields.get(2).getName());
	}

}

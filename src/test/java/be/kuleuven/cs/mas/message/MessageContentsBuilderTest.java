package be.kuleuven.cs.mas.message;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.reflect.Whitebox;

public class MessageContentsBuilderTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	AgentMessageBuilder builder;
	
	Field field1;
	Field field2;
	Field invalidField;
	
	@Before
	public void setUp() throws Exception {
		builder = new AgentMessageBuilder();
		field1 = new Field("name", "agent01");
		field2 = new Field("hold");
		invalidField = new Field("waiting-time" + AgentMessage.FIELD_SEP, "50s");
	}

	@Test
	public void isEmptyTest() {
		assertTrue(builder.isEmpty());
		builder.addField(new Field("name", "agent01"));
		assertFalse(builder.isEmpty());
	}
	
	@Test
	public void isValidFieldTrueNoValueTest() {
		Field field = new Field("hold");
		assertTrue(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldTrueWithValueTest() {
		Field field = new Field("name", "agent01");
		assertTrue(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldFalseNullTest() {
		assertFalse(builder.isValidField(null));
	}
	
	@Test
	public void isValidFieldFalseFieldSepInNameNoValueTest() {
		Field field = new Field("name" + AgentMessage.FIELD_SEP);
		assertFalse(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldFalseFieldSepInNameWithValueTest() {
		Field field = new Field("name" + AgentMessage.FIELD_SEP, "agent01");
		assertFalse(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldFalseNameValSepInNameNoValueTest() {
		Field field = new Field("name" + AgentMessage.NAME_VALUE_SEP);
		assertFalse(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldFalseNameValSepInNameWithValueTest() {
		Field field = new Field("name" + AgentMessage.NAME_VALUE_SEP, "agent01");
		assertFalse(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldFalseBothSepsInNameNoValueTest() {
		Field field = new Field("name" + AgentMessage.NAME_VALUE_SEP + "agent01" + AgentMessage.FIELD_SEP);
		assertFalse(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldFalseBothSepsInNameWithValueTest() {
		Field field = new Field("name" + AgentMessage.NAME_VALUE_SEP + "agent01" + AgentMessage.FIELD_SEP, "agent01");
		assertFalse(builder.isValidField(field));
	}

	@Test
	public void isValidFieldFalseFieldSepInValueTest() {
		Field field = new Field("name", "agent01" + AgentMessage.FIELD_SEP);
		assertFalse(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldFalseNameValSepInValueTest() {
		Field field = new Field("name", "agent01" + AgentMessage.NAME_VALUE_SEP);
		assertFalse(builder.isValidField(field));
	}
	
	@Test
	public void isValidFieldFalseNameBothSepsInValueTest() {
		Field field = new Field("name", "name" + AgentMessage.NAME_VALUE_SEP + "agent01" + AgentMessage.FIELD_SEP);
		assertFalse(builder.isValidField(field));
	}
	
	@Test
	public void addFieldInvalidTest() {
		exception.expect(IllegalArgumentException.class);
		builder.addField(new Field("name" + AgentMessage.FIELD_SEP));
		assertTrue(builder.isEmpty());
	}
	
	@Test
	public void addFieldsNullListTest() {
		exception.expect(IllegalArgumentException.class);
		builder.addFields(null);
	}
	
	@Test
	public void addFieldsInvalidTest() {
		exception.expect(IllegalArgumentException.class);
		builder.addFields(Arrays.asList(field1, field2, invalidField));
		assertTrue(builder.isEmpty());
	}
	
	@Test
	public void buildEmptyTest() {
		exception.expect(IllegalStateException.class);
		builder.build();
	}
	
	@Test
	public void buildValidTest() throws Exception {
		builder.addField(field1);
		AgentMessage msg = builder.build();
		assertEquals("name" + AgentMessage.NAME_VALUE_SEP + "agent01" + AgentMessage.FIELD_SEP, Whitebox.invokeMethod(msg, "getMessage"));
		
		assertTrue(builder.isEmpty());
		builder.addFields(Arrays.asList(field1, field2));
		msg = builder.build();
		List<Field> fields = msg.getContents();
		assertEquals(2, fields.size());
		assertEquals(field1, fields.get(0));
		assertEquals(field2, fields.get(1));
	}
}

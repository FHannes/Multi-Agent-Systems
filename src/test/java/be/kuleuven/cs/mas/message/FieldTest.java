package be.kuleuven.cs.mas.message;

import static org.junit.Assert.*;

import org.hamcrest.junit.ExpectedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FieldTest {
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	Field emptyVal;
	Field testField;
	
	@Before
	public void setUp() throws Exception {
		emptyVal = new Field("foo");
		testField = new Field("name", "agent01");
	}

	@Test
	public void constructorNullNameTest() {
		exception.expect(IllegalArgumentException.class);
		new Field(null);
	}
	
	@Test
	public void constructorNullValueTest() {
		exception.expect(IllegalArgumentException.class);
		new Field("", null);
	}
	
	@Test
	public void constructorValidNoValueTest() {
		Field field = new Field("foo");
		assertEquals("foo", field.getName());
		assertFalse(field.hasValue());
	}
	
	@Test
	public void constructorValidNonEmptyValueTest() {
		Field field = new Field("foo", "bar");
		assertEquals("foo", field.getName());
		assertTrue(field.hasValue());
		assertEquals("bar", field.getValue());
	}
	
	@Test
	public void tryGetEmptyValueTest() {
		exception.expect(IllegalStateException.class);
		emptyVal.getValue();
	}
	
	@Test
	public void toStringTest() {
		assertEquals("name" + AgentMessage.NAME_VALUE_SEP + "agent01" + AgentMessage.FIELD_SEP, testField.toString());
		System.out.println(testField.toString());
		assertEquals("foo" + AgentMessage.FIELD_SEP, emptyVal.toString());
	}
	
	@Test
	public void equalsTest() {
		assertEquals(testField, new Field("name", "agent01"));
		assertEquals(emptyVal, new Field("foo"));
		assertNotEquals(emptyVal, testField);
	}
}

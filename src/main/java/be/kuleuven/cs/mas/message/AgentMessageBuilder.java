package be.kuleuven.cs.mas.message;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;

public class AgentMessageBuilder {

	private static final Pattern REGEX = Pattern.compile(AgentMessage.FIELD_SEP + "|" + AgentMessage.NAME_VALUE_SEP);
	
	private StringBuilder sBuilder = new StringBuilder();
	
	private StringBuilder getSBuilder() {
		return this.sBuilder;
	}
	
	public AgentMessage build() throws IllegalStateException {
		AgentMessage toReturn;
		
		if (this.isEmpty()) {
			throw new IllegalStateException("cannot build empty message");
		}
		
		toReturn = new AgentMessage(this.getSBuilder().toString());
		this.getSBuilder().setLength(0); // clear string builder
		return toReturn;
	}
	
	public boolean isEmpty() {
		return this.getSBuilder().length() == 0;
	}
	
	public boolean isValidField(Field field) {
		if (field == null) {
			return false;
		}
		
		Matcher nameMatcher = REGEX.matcher(field.getName());
		if (nameMatcher.find()) {
			return false;
		} else if (! field.hasValue()) {
			return true;
		} else return ! REGEX.matcher(field.getValue()).find();
	}
	
	public AgentMessageBuilder addField(String name) throws IllegalArgumentException {
		return this.addField(new Field(name));
	}
	
	public AgentMessageBuilder addField(String name, String value) throws IllegalArgumentException {
		return this.addField(new Field(name, value));
	}
	
	public AgentMessageBuilder addField(Field field) throws IllegalArgumentException {
		if (! this.isValidField(field)) {
			throw new IllegalArgumentException("neither name nor value can contain field separator or name-value separator (or field was null)");
		}
		
		this.append(field);
		return this;
	}
	
	public AgentMessageBuilder addFields(List<Field> fields) throws IllegalArgumentException {
		if (fields == null) {
			throw new IllegalArgumentException("fields cannot be null");
		}
		
		// either add all fields or none of them
		for (Field field : fields) {
			if (! this.isValidField(field)) {
				throw new IllegalArgumentException("fields had invalid field");
			}
		}
		
		for (Field field : fields) {
			this.append(field);
		}
		return this;
	}
	
	private void append(Field field) {
		this.getSBuilder().append(field.toString());
	}
}

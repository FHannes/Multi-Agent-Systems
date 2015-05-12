package be.kuleuven.cs.mas.message;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageContentsBuilder {

	private static final Pattern REGEX = Pattern.compile(MessageContents.FIELD_SEP + "|" + MessageContents.NAME_VALUE_SEP);
	
	private StringBuilder sBuilder = new StringBuilder();
	
	private StringBuilder getSBuilder() {
		return this.sBuilder;
	}
	
	public MessageContents build() throws IllegalStateException {
		MessageContents toReturn;
		
		if (this.isEmpty()) {
			throw new IllegalStateException("cannot build empty message");
		}
		
		toReturn = new MessageContents(this.getSBuilder().toString());
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
	
	public void addField(Field field) throws IllegalArgumentException {
		if (! this.isValidField(field)) {
			throw new IllegalArgumentException("neither name nor value can contain field separator or name-value separator (or field was null)");
		}
		
		this.append(field);
	}
	
	public void addFields(List<Field> fields) throws IllegalArgumentException {
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
	}
	
	private void append(Field field) {
		this.getSBuilder().append(field.toString());
	}
}

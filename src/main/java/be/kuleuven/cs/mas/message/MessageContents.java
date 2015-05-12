package be.kuleuven.cs.mas.message;

import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Optional;

public class MessageContents {

	public static final String FIELD_SEP = ";";
	public static final String NAME_VALUE_SEP = "=";
	
	MessageContents(String message) throws IllegalArgumentException {
		if (message == null) {
			throw new IllegalArgumentException("message cannot be null");
		}
		this.message = message;
	}
	
	private String message;
	
	private String getMessage() {
		return this.message;
	}
	
	public List<Field> getContents() {
		List<Field> toReturn = new LinkedList<>();
		
		String[] fields = this.getMessage().split(FIELD_SEP);
		for (String field : fields) {
			String[] nameVal = field.split(NAME_VALUE_SEP);
			if (nameVal.length == 1) {
				toReturn.add(new Field(nameVal[0]));
			} else {
				toReturn.add(new Field(nameVal[0], nameVal[1]));
			}
		}
		
		return toReturn;
	}
}

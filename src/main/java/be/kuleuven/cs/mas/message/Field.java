package be.kuleuven.cs.mas.message;

import com.google.common.base.Optional;

import java.util.Objects;

public class Field {

	public Field(String name, String value) throws IllegalArgumentException {
		if (name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("value cannot be null");
		}
		this.name = name;
		this.value = Optional.of(value);
	}
	
	public Field(String name) throws IllegalArgumentException {
		if (name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}
		this.name = name;
		this.value = Optional.absent();
	}
	
	private String name;
	private Optional<String> value;
	
	public String getName() {
		return this.name;
	}
	
	public String getValue() throws IllegalStateException {
		if (! this.hasValue()) {
			throw new IllegalStateException("field has no value");
		}
		return this.getValuePriv().get();
	}
	
	private Optional<String> getValuePriv() {
		return this.value;
	}
	
	public boolean hasValue() {
		return this.getValuePriv().isPresent();
	}
	
	public String toString() {
		if (this.hasValue()) {
			return this.getName() + AgentMessage.NAME_VALUE_SEP + this.getValue() + AgentMessage.FIELD_SEP;
		} else {
			return this.getName() + AgentMessage.FIELD_SEP;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Field other = (Field) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}

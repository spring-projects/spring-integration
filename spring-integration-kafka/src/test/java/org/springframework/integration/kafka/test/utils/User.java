package org.springframework.integration.kafka.test.utils;

import org.apache.avro.specific.SpecificRecord;

/**
 * @author Soby Chacko
 * @since 0.5
 *        <p/>
 *        This class is copied (partly) from an Avro generated class for necessary testing.
 *        Please use caution when modify.
 */
public class User extends org.apache.avro.specific.SpecificRecordBase implements SpecificRecord {

	public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"org.springframework.integration.samples.kafka.user\",\"fields\":[{\"name\":\"firstName\",\"type\":\"string\"},{\"name\":\"lastName\",\"type\":\"string\"}]}");
	public java.lang.CharSequence firstName;
	public java.lang.CharSequence lastName;

	/**
	 * Default constructor.
	 */
	public User() {
	}

	/**
	 * All-args constructor.
	 */
	public User(java.lang.CharSequence firstName, java.lang.CharSequence lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public org.apache.avro.Schema getSchema() {
		return SCHEMA$;
	}

	// Used by DatumWriter.  Applications should not call.
	public java.lang.Object get(int field$) {
		switch (field$) {
			case 0:
				return firstName;
			case 1:
				return lastName;
			default:
				throw new org.apache.avro.AvroRuntimeException("Bad index");
		}
	}

	// Used by DatumReader.  Applications should not call.
	public void put(int field$, java.lang.Object value$) {
		switch (field$) {
			case 0:
				firstName = (java.lang.CharSequence) value$;
				break;
			case 1:
				lastName = (java.lang.CharSequence) value$;
				break;
			default:
				throw new org.apache.avro.AvroRuntimeException("Bad index");
		}
	}

	/**
	 * Gets the value of the 'firstName' field.
	 */
	public java.lang.CharSequence getFirstName() {
		return firstName;
	}

	/**
	 * Sets the value of the 'firstName' field.
	 *
	 * @param value the value to set.
	 */
	public void setFirstName(java.lang.CharSequence value) {
		this.firstName = value;
	}

	/**
	 * Gets the value of the 'lastName' field.
	 */
	public java.lang.CharSequence getLastName() {
		return lastName;
	}

	/**
	 * Sets the value of the 'lastName' field.
	 *
	 * @param value the value to set.
	 */
	public void setLastName(java.lang.CharSequence value) {
		this.lastName = value;
	}
}

/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.mongodb;

import org.bson.types.ObjectId;

/**
 * @author Christoph Strobl
 * @author Artem Bilan
 *
 * @since 5.3
 */
public class Person {

	private ObjectId id;

	private String firstName;

	private int age;

	private Person friend;

	public Person() {
		this.id = new ObjectId();
	}

	@Override
	public String toString() {
		return "Person [id=" + this.id + ", firstName=" + this.firstName + ", age=" + this.age +
				", friend=" + this.friend + "]";
	}

	public Person(ObjectId id, String firstName) {
		this.id = id;
		this.firstName = firstName;
	}

	public Person(String firstName, int age) {
		this();
		this.firstName = firstName;
		this.age = age;
	}

	public Person(String firstName) {
		this();
		this.firstName = firstName;
	}

	public ObjectId getId() {
		return this.id;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public int getAge() {
		return this.age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Person getFriend() {
		return this.friend;
	}

	public void setFriend(Person friend) {
		this.friend = friend;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(getClass().equals(obj.getClass()))) {
			return false;
		}

		Person that = (Person) obj;

		return this.id != null && this.id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

}

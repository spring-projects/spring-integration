/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.util;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
public class Person implements Serializable {

	private Address address;

	private String name;

	public Person(Address address, String name) {
		this.address = address;
		this.name = name;
	}

	public Person() {
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Person person = (Person) o;
		return Objects.equals(this.address, person.address) &&
				Objects.equals(this.name, person.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.address, this.name);
	}

}

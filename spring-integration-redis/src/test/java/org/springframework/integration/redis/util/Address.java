/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.util;

import java.io.Serializable;
import java.util.Objects;

@SuppressWarnings("serial")
public class Address implements Serializable {

	private String address;

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Address() {
	}

	public Address(String address) {
		this.address = address;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Address address1 = (Address) o;
		return Objects.equals(this.address, address1.address);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.address);
	}

}

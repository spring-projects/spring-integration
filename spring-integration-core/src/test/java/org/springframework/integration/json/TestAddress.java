/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.json;

/**
 * @author Mark Fisher
 * @since 2.0
 */
class TestAddress {

	private volatile int number;

	private volatile String street;

	TestAddress() {
	}

	TestAddress(int number, String street) {
		this.number = number;
		this.street = street;
	}

	public int getNumber() {
		return this.number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public String getStreet() {
		return this.street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TestAddress that = (TestAddress) o;

		if (number != that.number) {
			return false;
		}
		if (street != null ? !street.equals(that.street) : that.street != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = number;
		result = 31 * result + (street != null ? street.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return this.number + " " + this.street;
	}

}

/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

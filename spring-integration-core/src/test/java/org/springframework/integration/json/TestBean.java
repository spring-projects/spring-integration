/*
 * Copyright 2016-2024 the original author or authors.
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

import java.util.Date;

public class TestBean {

	private String value = "foo";

	private boolean test = false;

	private long number = 42;

	private Date now = new Date();

	private TestChildBean child = new TestChildBean();

	public String getValue() {
		return value;
	}

	public boolean getTest() {
		return test;
	}

	public long getNumber() {
		return number;
	}

	public Date getNow() {
		return this.now;
	}

	public TestChildBean getChild() {
		return child;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	public void setNumber(long number) {
		this.number = number;
	}

	public void setChild(TestChildBean child) {
		this.child = child;
	}

	public void setNow(Date now) {
		this.now = now;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((child == null) ? 0 : child.hashCode());
		result = prime * result + (int) (number ^ (number >>> 32));
		result = prime * result + (test ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TestBean other = (TestBean) obj;
		if (child == null) {
			if (other.child != null) {
				return false;
			}
		}
		else if (!child.equals(other.child)) {
			return false;
		}
		if (number != other.number) {
			return false;
		}
		if (test != other.test) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		}
		else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

}

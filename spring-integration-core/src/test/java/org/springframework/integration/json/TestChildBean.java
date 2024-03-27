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

public class TestChildBean {

	private String value = "bar";

	private String baz = null;

	private TestBean parent = null;

	public String getValue() {
		return value;
	}

	public String getBaz() {
		return baz;
	}

	public TestBean getParent() {
		return parent;
	}

	public void setParent(TestBean parent) {
		this.parent = parent;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setBaz(String baz) {
		this.baz = baz;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((baz == null) ? 0 : baz.hashCode());
		result = prime * result
				+ ((parent == null) ? 0 : parent.hashCode());
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
		TestChildBean other = (TestChildBean) obj;
		if (baz == null) {
			if (other.baz != null) {
				return false;
			}
		}
		else if (!baz.equals(other.baz)) {
			return false;
		}
		if (parent == null) {
			if (other.parent != null) {
				return false;
			}
		}
		else if (!parent.equals(other.parent)) {
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

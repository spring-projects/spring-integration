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
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestBean other = (TestBean) obj;
		if (child == null) {
			if (other.child != null)
				return false;
		} else if (!child.equals(other.child))
			return false;
		if (number != other.number)
			return false;
		if (test != other.test)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}

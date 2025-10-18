/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.jdbc.store.channel;

import java.io.Serializable;
import java.util.Objects;

/**
 * Test payload class for Jackson JSON serialization tests.
 * Represents a simple mail message.
 *
 * @author Yoobin Yoon
 */
public class TestMailMessage implements Serializable {

	private String subject;

	private String body;

	private String to;

	public TestMailMessage() {
	}

	public TestMailMessage(String subject, String body, String to) {
		this.subject = subject;
		this.body = body;
		this.to = to;
	}

	public String getSubject() {
		return this.subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return this.body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getTo() {
		return this.to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TestMailMessage that = (TestMailMessage) o;
		return Objects.equals(this.subject, that.subject) &&
				Objects.equals(this.body, that.body) &&
				Objects.equals(this.to, that.to);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.subject, this.body, this.to);
	}

	@Override
	public String toString() {
		return "TestMailMessage{" +
				"subject='" + this.subject + '\'' +
				", body='" + this.body + '\'' +
				", to='" + this.to + '\'' +
				'}';
	}

}

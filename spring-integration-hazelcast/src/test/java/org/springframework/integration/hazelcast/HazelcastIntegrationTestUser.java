/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.integration.hazelcast;

import java.io.Serializable;
import java.util.Objects;

/**
 * User Bean for Hazelcast Integration Unit Tests
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
public class HazelcastIntegrationTestUser implements Comparable<HazelcastIntegrationTestUser>, Serializable {

	private static final long serialVersionUID = -5357485957528362705L;

	private int id;

	private String name;

	private String surname;

	private int age;

	public HazelcastIntegrationTestUser(int id, String name, String surname) {
		this.id = id;
		this.name = name;
		this.surname = surname;
	}

	public HazelcastIntegrationTestUser(int id, String name, String surname, int age) {
		this.id = id;
		this.name = name;
		this.surname = surname;
		this.age = age;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", surname=" + surname + ", age="
				+ age + "]";
	}

	@Override
	public int compareTo(HazelcastIntegrationTestUser user) {
		return (this.id < user.getId()) ? -1 : (this.id > user.getId()) ? 1 : 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		HazelcastIntegrationTestUser that = (HazelcastIntegrationTestUser) o;
		return id == that.id &&
				age == that.age &&
				Objects.equals(name, that.name) &&
				Objects.equals(surname, that.surname);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, surname, age);
	}

}

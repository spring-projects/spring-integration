/*
 * Copyright 2020 the original author or authors.
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

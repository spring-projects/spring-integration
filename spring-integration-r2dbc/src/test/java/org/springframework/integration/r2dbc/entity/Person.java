/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.r2dbc.entity;

import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 *  @author Rohan Mukesh
 *  @author Artem Bilan
 *
 *  @since 5.4
 */
@Table
public class Person {

	@Id
	private Integer id;

	private String name;

	private Integer age;

	public void setId(Integer id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public Person(String name, Integer age) {
		this.name = name;
		this.age = age;
	}

	public Integer getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Integer getAge() {
		return this.age;
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
		return Objects.equals(this.id, person.id) &&
				Objects.equals(this.name, person.name) &&
				Objects.equals(this.age, person.age);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id, this.name, this.age);
	}

	@Override
	public String toString() {
		return "Person{" +
				"id=" + this.id +
				", name='" + this.name + '\'' +
				", age=" + this.age +
				'}';
	}

}

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

package org.springframework.integration.r2dbc.inbound;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.r2dbc.config.R2dbcDatabaseConfiguration;
import org.springframework.integration.r2dbc.entity.Person;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Rohan Mukesh
 * @author Artem Bilan
 *
 * @since 5.4
 */
@SpringJUnitConfig
@DirtiesContext
public class R2dbcMessageSourceTests {

	@Autowired
	DatabaseClient client;

	R2dbcEntityTemplate entityTemplate;

	@Autowired
	R2dbcMessageSource defaultR2dbcMessageSource;

	@Autowired
	R2dbcMessageSource r2dbcMessageSourceSelectOne;

	@Autowired
	R2dbcMessageSource r2dbcMessageSourceSelectMany;

	@Autowired
	R2dbcMessageSource r2dbcMessageSourceError;

	@BeforeEach
	public void setup() {
		Hooks.onOperatorDebug();
		entityTemplate = new R2dbcEntityTemplate(this.client);
		r2dbcMessageSourceSelectMany.setExpectSingleResult(false);
		defaultR2dbcMessageSource.setBindFunction(null);

		List<String> statements = Arrays.asList(
				"DROP TABLE IF EXISTS person;",
				"CREATE table person (id INT AUTO_INCREMENT NOT NULL, name VARCHAR2, age INT NOT NULL);");

		statements.forEach(it -> this.client.execute(it)
				.fetch()
				.rowsUpdated()
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete());
	}

	@Test
	public void validateSuccessfulQueryWithoutSettingExpectedElement() {
		this.entityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		StepVerifier.create((Flux<?>) r2dbcMessageSourceSelectMany.receive().getPayload())
				.assertNext(person -> assertThat(((Person) person).getName()).isEqualTo("Bob"))
				.verifyComplete();
	}


	@Test
	public void validateSuccessfulQueryWithSingleElementOfMonoDBObject() {
		this.entityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		r2dbcMessageSourceSelectOne.setExpectSingleResult(true);

		StepVerifier.create((Mono<?>) r2dbcMessageSourceSelectOne.receive().getPayload())
				.assertNext(person -> assertThat(((Person) person).getName()).isEqualTo("Bob"))
				.verifyComplete();

	}

	@Test
	public void validateSuccessfulQueryWithMultipleElementOfFluxDBObject() {
		this.entityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		this.entityTemplate.insert(new Person("Tom", 40))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		StepVerifier.create((Flux<?>) r2dbcMessageSourceSelectMany.receive().getPayload())
				.assertNext(person -> assertThat(((Person) person).getName()).isEqualTo("Bob"))
				.assertNext(person -> assertThat(((Person) person).getName()).isEqualTo("Tom"))
				.verifyComplete();

	}

	@Test
	public void validateSuccessfulUpdateWithSingleElementOfMonoDBObject() {
		this.entityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		r2dbcMessageSourceSelectMany.setUpdateSql("UPDATE Person SET name='Foo' where age = :age");
		r2dbcMessageSourceSelectMany.<Person>setBindFunction((bindSpec, o) -> bindSpec.bind("age", o.getAge()));
		r2dbcMessageSourceSelectMany.setExpectSingleResult(true);

		StepVerifier.create(r2dbcMessageSourceSelectMany.receive().getPayload())
				.assertNext(person -> assertThat(((Person) person).getName()).isEqualTo("Bob"))
				.verifyComplete();

		this.entityTemplate.select(Person.class)
				.all()
				.as(StepVerifier::create)
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Foo"))
				.verifyComplete();

	}

	@Test
	public void validateSuccessfulUpdateWithMultiplesElementsOfFluxDBObject() {
		this.entityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		this.entityTemplate.insert(new Person("Tom", 40))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		r2dbcMessageSourceSelectMany.setUpdateSql("UPDATE person SET name='Foo' where id = :id");
		r2dbcMessageSourceSelectMany.<Person>setBindFunction((bindSpec, o) -> bindSpec.bind("id", o.getId()));

		StepVerifier.create(r2dbcMessageSourceSelectMany.receive().getPayload())
				.expectNextCount(2)
				.verifyComplete();

		this.entityTemplate.select(Person.class)
				.all()
				.as(StepVerifier::create)
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Foo"))
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Foo"))
				.verifyComplete();

	}

	@Test
	public void validateSuccessfulUpdateWithoutBindFunction() {
		this.entityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		this.entityTemplate.insert(new Person("Tom", 40))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		r2dbcMessageSourceSelectMany.setUpdateSql("UPDATE person SET name='Foo' where id = 1");

		StepVerifier.create(r2dbcMessageSourceSelectMany.receive().getPayload())
				.expectNextCount(2)
				.verifyComplete();

		this.entityTemplate.select(Person.class)
				.all()
				.as(StepVerifier::create)
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Foo"))
				.assertNext(person -> assertThat(person.getName()).isEqualTo("Tom"))
				.verifyComplete();

	}

	@Test
	public void validateSuccessfulUpdateWithoutPayloadType() {
		this.entityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		this.entityTemplate.insert(new Person("Tom", 40))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		defaultR2dbcMessageSource.setUpdateSql("UPDATE person SET name='Foo' where id = 1");

		StepVerifier.create(defaultR2dbcMessageSource.receive().getPayload())
				.expectNextCount(2)
				.verifyComplete();

		this.client.execute("select * from person")
				.fetch()
				.all()
				.as(StepVerifier::create)
				.assertNext(person -> assertThat(person.get("name")).isEqualTo("Foo"))
				.assertNext(person -> assertThat(person.get("name")).isEqualTo("Tom"))
				.verifyComplete();

	}

	@Test
	public void testWrongPayloadTypeInBindFunction() {
		this.entityTemplate.insert(new Person("Bob", 35))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		this.entityTemplate.insert(new Person("Tom", 40))
				.then()
				.as(StepVerifier::create)
				.verifyComplete();

		defaultR2dbcMessageSource.setUpdateSql("UPDATE person SET name='Foo' where id = 1");
		defaultR2dbcMessageSource.<Person>setBindFunction((bindSpec, o) -> bindSpec.bind("id", o.getId()));

		StepVerifier.create(defaultR2dbcMessageSource.receive().getPayload())
				.expectErrorMatches(throwable -> throwable instanceof ClassCastException)
				.verify();

	}


	@Test
	public void testAnyOtherObjectQueryExpression() {

		StepVerifier.create((Flux<?>) r2dbcMessageSourceError.receive().getPayload())
				.expectErrorMatches(throwable -> throwable instanceof IllegalStateException
						&& throwable.getMessage().contains("'queryExpression' must evaluate to String or"))
				.verify();
	}

	@Configuration
	@Import(R2dbcDatabaseConfiguration.class)
	static class R2dbcMessageSourceConfiguration {

		@Autowired
		DatabaseClient databaseClient;

		@Bean
		public R2dbcMessageSource defaultR2dbcMessageSource() {
			return new R2dbcMessageSource(databaseClient, "select * from " +
					"person");
		}

		@Bean
		public R2dbcMessageSource r2dbcMessageSourceSelectOne() {
			R2dbcMessageSource r2dbcMessageSourceSelectOne = new R2dbcMessageSource(databaseClient,
					"select * from person Where id = 1");
			r2dbcMessageSourceSelectOne.setPayloadType(Person.class);
			return r2dbcMessageSourceSelectOne;
		}

		@Bean
		public R2dbcMessageSource r2dbcMessageSourceSelectMany() {
			R2dbcMessageSource r2dbcMessageSourceSelectMany = new R2dbcMessageSource(databaseClient, "select * from " +
					"person");
			r2dbcMessageSourceSelectMany.setPayloadType(Person.class);
			return r2dbcMessageSourceSelectMany;
		}

		@Bean
		public R2dbcMessageSource r2dbcMessageSourceError() {
			R2dbcMessageSource r2dbcMessageSourceError = new R2dbcMessageSource(databaseClient,
					new ValueExpression<>(new Object()));
			r2dbcMessageSourceError.setPayloadType(Person.class);
			return r2dbcMessageSourceError;
		}

	}

}
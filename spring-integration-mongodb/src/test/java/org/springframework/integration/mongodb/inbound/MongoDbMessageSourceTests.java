/*
 * Copyright 2007-2021 the original author or authors.
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

package org.springframework.integration.mongodb.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.bson.conversions.Bson;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;

import com.mongodb.BasicDBObject;

/**
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Yaron Yamin
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class MongoDbMessageSourceTests extends MongoDbAvailableTests {

	@Test
	public void withNullMongoDBFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageSource((MongoDatabaseFactory) null, mock(Expression.class)));
	}

	@Test
	public void withNullMongoTemplate() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageSource((MongoOperations) null, mock(Expression.class)));
	}

	@Test
	public void withNullQueryExpression() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageSource(mock(MongoDatabaseFactory.class), null));
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfulQueryWithSingleElementIfOneInListAsDbObject() {
		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<BasicDBObject> results = ((List<BasicDBObject>) messageSource.receive().getPayload());
		assertThat(results.size()).isEqualTo(1);
		BasicDBObject resultObject = results.get(0);

		assertThat(resultObject.get("name")).isEqualTo("Oleg");
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfulQueryWithSingleElementIfOneInList() {
		MongoDatabaseFactory mongoDbFactory = prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<Person> results = ((List<Person>) messageSource.receive().getPayload());
		assertThat(results.size()).isEqualTo(1);
		Person person = results.get(0);
		assertThat(person.getName()).isEqualTo("Oleg");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfulQueryWithSingleElementIfOneInListAndSingleResult() {
		MongoDatabaseFactory mongoDbFactory = prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setExpectSingleResult(true);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		Person person = (Person) messageSource.receive().getPayload();

		assertThat(person.getName()).isEqualTo("Oleg");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}


	@Test
	@MongoDbAvailable
	public void validateSuccessfulSubObjectQueryWithSingleElementIfOneInList() {
		MongoDatabaseFactory mongoDbFactory = prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<Person> results = ((List<Person>) messageSource.receive().getPayload());
		Person person = results.get(0);
		assertThat(person.getName()).isEqualTo("Oleg");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfulQueryWithMultipleElements() {
		List<Person> persons = queryMultipleElements(new LiteralExpression("{'address.state' : 'PA'}"));
		assertThat(persons.size()).isEqualTo(3);
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfulStringQueryExpressionWithMultipleElements() {
		List<Person> persons = queryMultipleElements(new SpelExpressionParser()
				.parseExpression("\"{'address.state' : 'PA'}\""));
		assertThat(persons.size()).isEqualTo(3);
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfulBasicQueryExpressionWithMultipleElements() {
		List<Person> persons = queryMultipleElements(new SpelExpressionParser()
				.parseExpression("new BasicQuery(\"{'address.state' : 'PA'}\").limit(2)"));
		assertThat(persons.size()).isEqualTo(2);
	}

	@SuppressWarnings("unchecked")
	private List<Person> queryMultipleElements(Expression queryExpression) {
		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Manny"), "data");
		template.save(this.createPerson("Moe"), "data");
		template.save(this.createPerson("Jack"), "data");

		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();

		return (List<Person>) messageSource.receive().getPayload();
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfulQueryWithNullReturn() {

		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Manny"), "data");
		template.save(this.createPerson("Moe"), "data");
		template.save(this.createPerson("Jack"), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'NJ'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		assertThat(messageSource.receive()).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	@MongoDbAvailable
	public void validateSuccessfulQueryWithCustomConverter() {

		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Manny"), "data");
		template.save(this.createPerson("Moe"), "data");
		template.save(this.createPerson("Jack"), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		MappingMongoConverter converter = new TestMongoConverter(mongoDbFactory, new MongoMappingContext());
		messageSource.setBeanFactory(mock(BeanFactory.class));
		converter.afterPropertiesSet();
		converter = spy(converter);
		messageSource.setMongoConverter(converter);
		messageSource.afterPropertiesSet();

		List<Person> persons = (List<Person>) messageSource.receive().getPayload();
		assertThat(persons.size()).isEqualTo(3);
		verify(converter, times(3)).read((Class<Person>) Mockito.any(), Mockito.any(Bson.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	@MongoDbAvailable
	public void validateSuccessfulQueryWithMongoTemplateAndUpdate() {
		MongoDatabaseFactory mongoDbFactory = prepareMongoFactory();
		MappingMongoConverter converter = new TestMongoConverter(mongoDbFactory, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);

		MongoTemplate template = new MongoTemplate(mongoDbFactory, converter);
		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(template, queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.setUpdateExpression(new LiteralExpression("{ $set: {'address.state' : 'NJ'} }"));
		messageSource.afterPropertiesSet();

		MongoTemplate writingTemplate = new MongoTemplate(mongoDbFactory, converter);
		writingTemplate.save(createPerson("Manny"), "data");
		writingTemplate.save(createPerson("Moe"), "data");
		writingTemplate.save(createPerson("Jack"), "data");

		List<Person> persons = (List<Person>) messageSource.receive().getPayload();
		assertThat(persons).hasSize(3);
		verify(converter, times(3)).read((Class<Person>) Mockito.any(), Mockito.any(Bson.class));

		assertThat(messageSource.receive()).isNull();

		assertThat(template.find(new BasicQuery("{'address.state' : 'NJ'}"), Object.class, "data"))
				.hasSize(3);
	}

	@Test
	@MongoDbAvailable
	public void validatePipelineInModifyOut() {

		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);

		template.save(BasicDBObject.parse("{'name' : 'Manny', 'id' : 1}"), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Manny'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setExpectSingleResult(true);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		BasicDBObject result = (BasicDBObject) messageSource.receive().getPayload();
		Object id = result.get("_id");
		result.put("company", "PepBoys");
		template.save(result, "data");
		result = (BasicDBObject) messageSource.receive().getPayload();
		assertThat(result.get("_id")).isEqualTo(id);
	}

}

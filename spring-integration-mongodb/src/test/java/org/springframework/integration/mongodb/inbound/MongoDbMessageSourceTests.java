/*
 * Copyright 2007-present the original author or authors.
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

import java.util.List;

import com.mongodb.BasicDBObject;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.test.support.TestApplicationContextAware;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Yaron Yamin
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 2.2
 *
 */
class MongoDbMessageSourceTests implements MongoDbContainerTest, TestApplicationContextAware {

	static MongoDatabaseFactory MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		MONGO_DATABASE_FACTORY = MongoDbContainerTest.createMongoDbFactory();
	}

	@Test
	void withNullMongoDBFactory() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageSource((MongoDatabaseFactory) null, mock(Expression.class)));
	}

	@Test
	void withNullMongoTemplate() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageSource((MongoOperations) null, mock(Expression.class)));
	}

	@Test
	void withNullQueryExpression() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageSource(mock(MongoDatabaseFactory.class), null));
	}

	@Test
	void validateSuccessfulQueryWithSingleElementIfOneInListAsDbObject() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		template.save(MongoDbContainerTest.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(MONGO_DATABASE_FACTORY, queryExpression);
		messageSource.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<BasicDBObject> results = ((List<BasicDBObject>) messageSource.receive().getPayload());
		assertThat(results).hasSize(1);
		BasicDBObject resultObject = results.get(0);

		assertThat(resultObject).containsEntry("name", "Oleg");
	}

	@Test
	void validateSuccessfulQueryWithSingleElementIfOneInList() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		template.save(MongoDbContainerTest.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(MONGO_DATABASE_FACTORY, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<Person> results = ((List<Person>) messageSource.receive().getPayload());
		assertThat(results).hasSize(1);
		Person person = results.get(0);
		assertThat(person.getName()).isEqualTo("Oleg");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	void validateSuccessfulQueryWithSingleElementIfOneInListAndSingleResult() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		template.save(MongoDbContainerTest.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(MONGO_DATABASE_FACTORY, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setExpectSingleResult(true);
		messageSource.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageSource.afterPropertiesSet();
		Person person = (Person) messageSource.receive().getPayload();

		assertThat(person.getName()).isEqualTo("Oleg");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	void validateSuccessfulSubObjectQueryWithSingleElementIfOneInList() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		template.save(MongoDbContainerTest.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(MONGO_DATABASE_FACTORY, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<Person> results = ((List<Person>) messageSource.receive().getPayload());
		Person person = results.get(0);
		assertThat(person.getName()).isEqualTo("Oleg");
		assertThat(person.getAddress().getState()).isEqualTo("PA");
	}

	@Test
	void validateSuccessfulQueryWithMultipleElements() {
		List<Person> persons = queryMultipleElements(new LiteralExpression("{'address.state' : 'PA'}"));
		assertThat(persons).hasSize(3);
	}

	@Test
	void validateSuccessfulStringQueryExpressionWithMultipleElements() {
		List<Person> persons = queryMultipleElements(new SpelExpressionParser()
				.parseExpression("\"{'address.state' : 'PA'}\""));
		assertThat(persons).hasSize(3);
	}

	@Test
	void validateSuccessfulBasicQueryExpressionWithMultipleElements() {
		List<Person> persons = queryMultipleElements(new SpelExpressionParser()
				.parseExpression("new BasicQuery(\"{'address.state' : 'PA'}\").limit(2)"));
		assertThat(persons).hasSize(2);
	}

	@SuppressWarnings("unchecked")
	private List<Person> queryMultipleElements(Expression queryExpression) {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		template.save(MongoDbContainerTest.createPerson("Manny"), "data");
		template.save(MongoDbContainerTest.createPerson("Moe"), "data");
		template.save(MongoDbContainerTest.createPerson("Jack"), "data");

		MongoDbMessageSource messageSource = new MongoDbMessageSource(MONGO_DATABASE_FACTORY, queryExpression);
		messageSource.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageSource.afterPropertiesSet();

		return (List<Person>) messageSource.receive().getPayload();
	}

	@Test
	void validateSuccessfulQueryWithNullReturn() {

		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		template.save(MongoDbContainerTest.createPerson("Manny"), "data");
		template.save(MongoDbContainerTest.createPerson("Moe"), "data");
		template.save(MongoDbContainerTest.createPerson("Jack"), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'NJ'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(MONGO_DATABASE_FACTORY, queryExpression);
		messageSource.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageSource.afterPropertiesSet();
		assertThat(messageSource.receive()).isNull();
	}

	@SuppressWarnings("unchecked")
	@Test
	void validateSuccessfulQueryWithCustomConverter() {

		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		template.save(MongoDbContainerTest.createPerson("Manny"), "data");
		template.save(MongoDbContainerTest.createPerson("Moe"), "data");
		template.save(MongoDbContainerTest.createPerson("Jack"), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(MONGO_DATABASE_FACTORY, queryExpression);
		MappingMongoConverter converter = new TestMongoConverter(MONGO_DATABASE_FACTORY, new MongoMappingContext());
		messageSource.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		converter.afterPropertiesSet();
		converter = spy(converter);
		messageSource.setMongoConverter(converter);
		messageSource.afterPropertiesSet();

		List<Person> persons = (List<Person>) messageSource.receive().getPayload();
		assertThat(persons).hasSize(3);
		verify(converter, times(3)).read((Class<Person>) Mockito.any(), Mockito.any(Bson.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	void validateSuccessfulQueryWithMongoTemplateAndUpdate() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);
		MappingMongoConverter converter = new TestMongoConverter(MONGO_DATABASE_FACTORY, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY, converter);
		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(template, queryExpression);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageSource.setUpdateExpression(new LiteralExpression("{ $set: {'address.state' : 'NJ'} }"));
		messageSource.afterPropertiesSet();

		MongoTemplate writingTemplate = new MongoTemplate(MONGO_DATABASE_FACTORY, converter);
		writingTemplate.save(MongoDbContainerTest.createPerson("Manny"), "data");
		writingTemplate.save(MongoDbContainerTest.createPerson("Moe"), "data");
		writingTemplate.save(MongoDbContainerTest.createPerson("Jack"), "data");

		List<Person> persons = (List<Person>) messageSource.receive().getPayload();
		assertThat(persons).hasSize(3);
		verify(converter, times(3)).read((Class<Person>) Mockito.any(), Mockito.any(Bson.class));

		assertThat(messageSource.receive()).isNull();

		assertThat(template.find(new BasicQuery("{'address.state' : 'NJ'}"), Object.class, "data"))
				.hasSize(3);
	}

	@Test
	void validatePipelineInModifyOut() {

		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);

		template.save(BasicDBObject.parse("{'name' : 'Manny', 'id' : 1}"), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Manny'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(MONGO_DATABASE_FACTORY, queryExpression);
		messageSource.setExpectSingleResult(true);
		messageSource.setApplicationContext(TEST_INTEGRATION_CONTEXT);
		messageSource.setBeanFactory(TEST_INTEGRATION_CONTEXT);
		messageSource.afterPropertiesSet();
		BasicDBObject result = (BasicDBObject) messageSource.receive().getPayload();
		Object id = result.get("_id");
		result.put("company", "PepBoys");
		template.save(result, "data");
		result = (BasicDBObject) messageSource.receive().getPayload();
		assertThat(result).containsEntry("_id", id);
	}

}

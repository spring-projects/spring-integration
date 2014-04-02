/*
 * Copyright 2007-2014 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.mongodb.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;

/**
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public class MongoDbMessageSourceTests extends MongoDbAvailableTests {
	/**
	 * Tests by providing a null MongoDB Factory
	 *
	 */
	@Test(expected=IllegalArgumentException.class)
	public void withNullMongoDBFactory() {
		Expression expression = mock(Expression.class);
		new MongoDbMessageSource((MongoDbFactory)null, expression);
	}

	@Test(expected=IllegalArgumentException.class)
	public void withNullMongoTemplate() {
		Expression expression = mock(Expression.class);
		new MongoDbMessageSource((MongoOperations)null, expression);
	}

	@Test(expected=IllegalArgumentException.class)
	public void withNullQueryExpression() {
		MongoDbFactory mongoDbFactory = mock(MongoDbFactory.class);
		new MongoDbMessageSource(mongoDbFactory, null);
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfullQueryWithSinigleElementIfOneInListAsDbObject() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<DBObject> results = ((List<DBObject>)messageSource.receive().getPayload());
		assertEquals(1, results.size());
		DBObject resultObject = results.get(0);

		assertEquals("Oleg", resultObject.get("name"));
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfullQueryWithSinigleElementIfOneInList() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<Person> results = ((List<Person>)messageSource.receive().getPayload());
		assertEquals(1, results.size());
		Person person = results.get(0);
		assertEquals("Oleg", person.getName());
		assertEquals("PA", person.getAddress().getState());
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfullQueryWithSinigleElementIfOneInListAndSingleResult() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Oleg'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setExpectSingleResult(true);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		Person person = (Person)messageSource.receive().getPayload();

		assertEquals("Oleg", person.getName());
		assertEquals("PA", person.getAddress().getState());
	}


	@Test
	@MongoDbAvailable
	public void validateSuccessfullSubObjectQueryWithSinigleElementIfOneInList() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson(), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setEntityClass(Object.class);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<Person> results = ((List<Person>)messageSource.receive().getPayload());
		Person person = results.get(0);
		assertEquals("Oleg", person.getName());
		assertEquals("PA", person.getAddress().getState());
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfullQueryWithMultipleElements() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Manny"), "data");
		template.save(this.createPerson("Moe"), "data");
		template.save(this.createPerson("Jack"), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		@SuppressWarnings("unchecked")
		List<Person> persons = (List<Person>) messageSource.receive().getPayload();
		assertEquals(3, persons.size());
	}

	@Test
	@MongoDbAvailable
	public void validateSuccessfullQueryWithNullReturn() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.save(this.createPerson("Manny"), "data");
		template.save(this.createPerson("Moe"), "data");
		template.save(this.createPerson("Jack"), "data");

		Expression queryExpression = new LiteralExpression("{'address.state' : 'NJ'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		assertNull(messageSource.receive());
	}

	@SuppressWarnings("unchecked")
	@Test
	@MongoDbAvailable
	public void validateSuccessfullQueryWithCustomConverter() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

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
		assertEquals(3, persons.size());
		verify(converter, times(3)).read((Class<Person>) Mockito.any(), Mockito.any(DBObject.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	@MongoDbAvailable
	public void validateSuccessfullQueryWithMongoTemplate() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

		MappingMongoConverter converter = new TestMongoConverter(mongoDbFactory, new MongoMappingContext());
		converter.afterPropertiesSet();
		converter = spy(converter);

		MongoTemplate template = new MongoTemplate(mongoDbFactory, converter);
		Expression queryExpression = new LiteralExpression("{'address.state' : 'PA'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(template, queryExpression);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();

		MongoTemplate writingTemplate = new MongoTemplate(mongoDbFactory, converter);
		writingTemplate.save(this.createPerson("Manny"), "data");
		writingTemplate.save(this.createPerson("Moe"), "data");
		writingTemplate.save(this.createPerson("Jack"), "data");

		List<Person> persons = (List<Person>) messageSource.receive().getPayload();
		assertEquals(3, persons.size());
		verify(converter, times(3)).read((Class<Person>) Mockito.any(), Mockito.any(DBObject.class));
	}

	@Test
	@MongoDbAvailable
	public void validatePipelineInModifyOut() throws Exception {

		MongoDbFactory mongoDbFactory = this.prepareMongoFactory();

		MongoTemplate template = new MongoTemplate(mongoDbFactory);

		template.save(JSON.parse("{'name' : 'Manny', 'id' : 1}"), "data");

		Expression queryExpression = new LiteralExpression("{'name' : 'Manny'}");
		MongoDbMessageSource messageSource = new MongoDbMessageSource(mongoDbFactory, queryExpression);
		messageSource.setExpectSingleResult(true);
		messageSource.setBeanFactory(mock(BeanFactory.class));
		messageSource.afterPropertiesSet();
		DBObject result = (DBObject) messageSource.receive().getPayload();
		Object id = result.get("_id");
		result.put("company","PepBoys");
		template.save(result, "data");
		result = (DBObject) messageSource.receive().getPayload();
		assertEquals(id, result.get("_id"));
	}
}

/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.mongodb.config.xml;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.query.Order;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mongodb.MessageWrapper;
import org.springframework.integration.mongodb.MongoDbIntegrationConstants;
import org.springframework.integration.mongodb.Person;
import org.springframework.integration.mongodb.inbound.MongoDbPollingChannelAdapter;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.test.util.TestUtils;

/**
 * The test class for the {@link MongoDbInboundChannelAdapterParser}
 *
 * @author Amol Nayak
 * @since 2.2
 */
public class MongoDbInboundChannelAdapterParserTests {

	private static ClassPathXmlApplicationContext context;


	@BeforeClass
	public static void setupContext() {
		context =
			new ClassPathXmlApplicationContext("classpath:org/springframework/integration/mongodb/config/xml/MongoDbInboundChannelAdapterParserTests-config.xml");
	}

	@AfterClass
	public static void destroyContext() {
		context.close();
	}

	//TODO: Add more tests to cover rest of the cases added in config
	/**
	 * The test adapter definition without the collection name specified
	 */
	@Test
	@MongoDbAvailable
	public void withNoCollectionSpecified() {
		SourcePollingChannelAdapter adapter = context.getBean("withoutCollection", SourcePollingChannelAdapter.class);
		MongoDbPollingChannelAdapter source = TestUtils.getPropertyValue(adapter, "source", MongoDbPollingChannelAdapter.class);
		Assert.assertEquals(MongoDbIntegrationConstants.DEFAULT_COLLECTION_NAME, TestUtils.getPropertyValue(source, "collection"));
		Class<?> entityClass = TestUtils.getPropertyValue(source, "entityClass", Class.class);
		Assert.assertNotNull(entityClass);
		Assert.assertEquals(MessageWrapper.class, entityClass);
		boolean deleteAfterSelect = TestUtils.getPropertyValue(source, "deleteAfterSelect", Boolean.class);
		Assert.assertEquals(false, deleteAfterSelect);
		Query query = TestUtils.getPropertyValue(source, "query",Query.class);
		Assert.assertNotNull(query);
		Object update = TestUtils.getPropertyValue(source, "update");
		Assert.assertNull(update);
	}

	/**
	 * The test  that sets both the collection name and the entity class
	 */
	@Test
	@MongoDbAvailable
	public void withEntityClassAndCollection() {
		SourcePollingChannelAdapter adapter = context.getBean("withEntityClassAndCollection", SourcePollingChannelAdapter.class);
		MongoDbPollingChannelAdapter source = TestUtils.getPropertyValue(adapter, "source", MongoDbPollingChannelAdapter.class);
		String collection = TestUtils.getPropertyValue(source, "collection",String.class);
		Assert.assertNotNull(collection);
		Assert.assertEquals("Person", collection);
		Class<?> entityClass = TestUtils.getPropertyValue(source, "entityClass", Class.class);
		Assert.assertNotNull(entityClass);
		Assert.assertEquals(Person.class, entityClass);
		boolean deleteAfterSelect = TestUtils.getPropertyValue(source, "deleteAfterSelect", Boolean.class);
		Assert.assertEquals(false, deleteAfterSelect);
		Query query = TestUtils.getPropertyValue(source, "query",Query.class);
		Assert.assertNotNull(query);
		Object update = TestUtils.getPropertyValue(source, "update");
		Assert.assertNull(update);
	}

	/**
	 * The junit test case where both select and update statements along with the collection name
	 */
	@Test
	@MongoDbAvailable
	public void withCollectionSelectAndUpdate() {
		SourcePollingChannelAdapter adapter = context.getBean("withCollectionSelectAndUpdate", SourcePollingChannelAdapter.class);
		MongoDbPollingChannelAdapter source = TestUtils.getPropertyValue(adapter, "source", MongoDbPollingChannelAdapter.class);
		String collection = TestUtils.getPropertyValue(source, "collection",String.class);
		Assert.assertNotNull(collection);
		Assert.assertEquals("Person", collection);
		Class<?> entityClass = TestUtils.getPropertyValue(source, "entityClass", Class.class);
		Assert.assertNotNull(entityClass);
		Assert.assertEquals(Person.class, entityClass);
		Query query = TestUtils.getPropertyValue(source, "query",Query.class);
		Assert.assertNotNull(query);
		Object update = TestUtils.getPropertyValue(source, "update");
		Assert.assertNotNull(update);
	}

	/**
	 * The junit test case where both select and update statements. The collection used is default
	 */
	@Test
	@MongoDbAvailable
	public void withOnlySelectAndUpdate() {
		SourcePollingChannelAdapter adapter = context.getBean("withOnlySelectAndUpdate", SourcePollingChannelAdapter.class);
		MongoDbPollingChannelAdapter source = TestUtils.getPropertyValue(adapter, "source", MongoDbPollingChannelAdapter.class);
		String collection = TestUtils.getPropertyValue(source, "collection",String.class);
		Assert.assertNotNull(collection);
		Assert.assertEquals(MongoDbIntegrationConstants.DEFAULT_COLLECTION_NAME, collection);
		Class<?> entityClass = TestUtils.getPropertyValue(source, "entityClass", Class.class);
		Assert.assertNotNull(entityClass);
		Assert.assertEquals(Person.class, entityClass);
		Query query = TestUtils.getPropertyValue(source, "query",Query.class);
		Assert.assertNotNull(query);
		Object update = TestUtils.getPropertyValue(source, "update");
		Assert.assertNotNull(update);
	}




	/**
	 * The test  that tests with the delete after select
	 */
	@Test
	@MongoDbAvailable
	public void withDeleteAfterSelect() {
		SourcePollingChannelAdapter adapter = context.getBean("withDeleteAfterSelect", SourcePollingChannelAdapter.class);
		MongoDbPollingChannelAdapter source = TestUtils.getPropertyValue(adapter, "source", MongoDbPollingChannelAdapter.class);
		String collection = TestUtils.getPropertyValue(source, "collection",String.class);
		Assert.assertNotNull(collection);
		Assert.assertEquals("Person", collection);
		Class<?> entityClass = TestUtils.getPropertyValue(source, "entityClass", Class.class);
		Assert.assertNotNull(entityClass);
		Assert.assertEquals(Person.class, entityClass);
		boolean deleteAfterSelect = TestUtils.getPropertyValue(source, "deleteAfterSelect", Boolean.class);
		Assert.assertEquals(true, deleteAfterSelect);
		Query query = TestUtils.getPropertyValue(source, "query",Query.class);
		Assert.assertNotNull(query);
		Object update = TestUtils.getPropertyValue(source, "update");
		Assert.assertNull(update);
	}

	/**
	 * The test case which tests when the sort-on attribute is specified in the xml
	 */
	@Test
	@MongoDbAvailable
	public void withOnlySortOn() {
		SourcePollingChannelAdapter adapter = context.getBean("withOnlySortOn", SourcePollingChannelAdapter.class);
		MongoDbPollingChannelAdapter source = TestUtils.getPropertyValue(adapter, "source", MongoDbPollingChannelAdapter.class);
		String sortOn = TestUtils.getPropertyValue(source, "sortOn", String.class);
		Assert.assertEquals(sortOn, "age");
		Order sortOrder = TestUtils.getPropertyValue(source, "sortOrder", Order.class);
		Assert.assertEquals(Order.ASCENDING, sortOrder);
	}

	/**
	 * The test case that tests when both the sort-on and sort-order attributes are specified
	 */
	@Test
	@MongoDbAvailable
	public void withSortOnAndOrderBy() {
		SourcePollingChannelAdapter adapter = context.getBean("withSortAndOrderOn", SourcePollingChannelAdapter.class);
		MongoDbPollingChannelAdapter source = TestUtils.getPropertyValue(adapter, "source", MongoDbPollingChannelAdapter.class);
		String sortOn = TestUtils.getPropertyValue(source, "sortOn", String.class);
		Assert.assertEquals(sortOn, "age");
		Order sortOrder = TestUtils.getPropertyValue(source, "sortOrder", Order.class);
		Assert.assertEquals(Order.DESCENDING, sortOrder);
	}

	/**
	 * The test case that tests when the limit attribute is set
	 */
	@Test
	@MongoDbAvailable
	public void withLimitAttribute() {
		SourcePollingChannelAdapter adapter = context.getBean("withLimitOn", SourcePollingChannelAdapter.class);
		MongoDbPollingChannelAdapter source = TestUtils.getPropertyValue(adapter, "source", MongoDbPollingChannelAdapter.class);
		int limit = TestUtils.getPropertyValue(source, "limit",Integer.class);
		Assert.assertEquals(1000, limit);
	}
}

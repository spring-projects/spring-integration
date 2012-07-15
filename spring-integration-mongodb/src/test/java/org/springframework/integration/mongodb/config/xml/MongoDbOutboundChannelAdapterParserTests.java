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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.mongodb.outbound.MongoDbMessageHandler;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.test.util.TestUtils;

import com.mongodb.WriteConcern;

/**
 * The test class for the Mongo DB outbound adapter
 * @author Amol Nayak
 *
 * @since 2.2
 *
 */
public class MongoDbOutboundChannelAdapterParserTests {

	private static ClassPathXmlApplicationContext context;

	private EventDrivenConsumer consumer;

	@BeforeClass
	public static void setupContext() {
		context =
			new ClassPathXmlApplicationContext("classpath:org/springframework/integration/mongodb/config/xml/MongoDbOutboundChannelAdapterParserTests-context.xml");
	}

	@AfterClass
	public static void destroyContext() {
		context.close();
	}

	public void setup(String beanId) {
		consumer = context.getBean(beanId, EventDrivenConsumer.class);
	}

	@Test
	@MongoDbAvailable
	public void configWithWriteResultCheckingAndWriteConcern() {
		setup("configWithWriteResultCheckingAndWriteConcern");
		MongoDbMessageHandler handler = TestUtils.getPropertyValue(consumer, "handler", MongoDbMessageHandler.class);
		Assert.assertNotNull(handler);
		WriteResultChecking checking = TestUtils.getPropertyValue(handler, "writeResultChecking", WriteResultChecking.class);
		Assert.assertEquals(WriteResultChecking.LOG, checking);
		WriteConcern concern = TestUtils.getPropertyValue(handler, "writeConcern", WriteConcern.class);
		Assert.assertEquals("MAJORITY", concern.getWString());
		Assert.assertEquals("SomeCollection", TestUtils.getPropertyValue(handler, "collection", String.class));
		Assert.assertNotNull(TestUtils.getPropertyValue(handler, "factory"));
	}

	@Test
	@MongoDbAvailable
	public void configWithNoCollection() {
		setup("configWithNoCollection");
		MongoDbMessageHandler handler = TestUtils.getPropertyValue(consumer, "handler", MongoDbMessageHandler.class);
		Assert.assertNotNull(handler);
		Assert.assertNull(TestUtils.getPropertyValue(handler, "collection"));
	}
}

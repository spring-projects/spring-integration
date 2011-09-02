/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gemfire.store;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests the Gemfire
 * {@link org.springframework.integration.store.MessageGroupStore}
 * implementation,
 * {@link org.springframework.integration.gemfire.store.GemfireMessageGroupStore}
 * .
 * <p/>
 * It tests the {@link org.springframework.integration.store.MessageGroupStore}
 * by sending 10 batches of letters (all of the same width), and then counting
 * on the other end that indeed all 10 batches arrived and that all letters
 * expected are there. *
 * 
 * @author Josh Long
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GemfireMessageGroupStoreTests {

	@Autowired
	private GemfireMessageGroupStoreTestConfiguration.FakeMessageConsumer consumer;
	@Autowired
	private GemfireMessageGroupStoreTestConfiguration.FakeMessageProducer producer;

	private List<String> letters = GemfireMessageGroupStoreTestConfiguration.LIST_OF_STRINGS;

	private int maxSize = 10;

	@Test
	public void testGemfireMessageGroupStore() throws Exception {
			producer.afterPropertiesSet();
			producer.start();
			List<Collection<Object>> batches = consumer.getBatches();
			assertEquals(maxSize, batches.size());
			for (Collection<Object> collection : batches) {
				Assert.assertTrue(letters.size() == collection.size());
				for (String c : this.letters) {
					Assert.assertTrue(collection.contains(c));
				}
				for (Object o : collection) {
					Assert.assertTrue(o instanceof String);
				}
			}
			producer.stop();
	}
}
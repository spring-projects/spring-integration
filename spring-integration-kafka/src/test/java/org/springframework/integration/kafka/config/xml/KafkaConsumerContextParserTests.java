/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.config.xml;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.kafka.support.ConsumerMetadata;
import org.springframework.integration.kafka.support.KafkaConsumerContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Soby Chacko
 * @since 0.5
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KafkaConsumerContextParserTests<K,V> {

	@Autowired
	private ApplicationContext appContext;

	@Test
	@SuppressWarnings("unchecked")
	public void testConsumerContextConfiguration() {
		final KafkaConsumerContext<K,V> consumerContext = appContext.getBean("consumerContext", KafkaConsumerContext.class);
		Assert.assertNotNull(consumerContext);

		final ConsumerMetadata<K,V> cm = appContext.getBean("consumerMetadata_default1", ConsumerMetadata.class);
		Assert.assertNotNull(cm);
	}
}

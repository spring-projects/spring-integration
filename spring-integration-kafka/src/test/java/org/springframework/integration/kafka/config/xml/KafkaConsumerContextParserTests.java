/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import kafka.consumer.Blacklist;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.kafka.support.ConsumerConfiguration;
import org.springframework.integration.kafka.support.ConsumerMetadata;
import org.springframework.integration.kafka.support.KafkaConsumerContext;
import org.springframework.integration.kafka.support.TopicFilterConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Soby Chacko
 * @author Artem Bilan
 * @since 0.5
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KafkaConsumerContextParserTests<K, V> {

	@Autowired
	private ApplicationContext appContext;

	@Test
	@SuppressWarnings("unchecked")
	public void testConsumerContextConfiguration() {
		final KafkaConsumerContext<K, V> consumerContext = appContext.getBean("consumerContext",
				KafkaConsumerContext.class);
		Assert.assertNotNull(consumerContext);
		ConsumerConfiguration<K, V> cc = consumerContext.getConsumerConfiguration("default1");
		ConsumerMetadata<K, V> cm = cc.getConsumerMetadata();
		assertNotNull(cm);
		TopicFilterConfiguration topicFilterConfiguration = cm.getTopicFilterConfiguration();
		assertEquals("foo : 10", topicFilterConfiguration.toString());
		assertThat(topicFilterConfiguration.getTopicFilter(), Matchers.instanceOf(Blacklist.class));
	}

}

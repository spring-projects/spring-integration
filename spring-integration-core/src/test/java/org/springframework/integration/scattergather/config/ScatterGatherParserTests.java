/*
 * Copyright 2014-2017 the original author or authors.
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

package org.springframework.integration.scattergather.config;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.Lifecycle;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.scattergather.ScatterGatherHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @author Abdul Zaheer
 *
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ScatterGatherParserTests {

	@Autowired
	private BeanFactory beanFactory;

	@Test
	public void testAuction() {
		MessageHandler scatterGather = this.beanFactory.getBean("scatterGather1.handler", MessageHandler.class);
		assertThat(scatterGather, instanceOf(ScatterGatherHandler.class));
		assertSame(this.beanFactory.getBean("scatterChannel"),
				TestUtils.getPropertyValue(scatterGather, "scatterChannel"));
		assertTrue(this.beanFactory.containsBean("scatterGather1.gatherer"));
		AggregatingMessageHandler gatherer =
				this.beanFactory.getBean("scatterGather1.gatherer", AggregatingMessageHandler.class);
		assertSame(gatherer, TestUtils.getPropertyValue(scatterGather, "gatherer"));

		Object reaper = this.beanFactory.getBean("reaper");
		assertSame(gatherer.getMessageStore(), TestUtils.getPropertyValue(reaper, "messageGroupStore"));
		assertTrue(TestUtils.getPropertyValue(scatterGather, "requiresReply", Boolean.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDistribution() {
		MessageHandler scatterGather = this.beanFactory.getBean("scatterGather2.handler", MessageHandler.class);
		assertSame(this.beanFactory.getBean("gatherChannel"),
				TestUtils.getPropertyValue(scatterGather, "gatherChannel"));
		Lifecycle gatherEndpoint = TestUtils.getPropertyValue(scatterGather, "gatherEndpoint", Lifecycle.class);
		assertNotNull(TestUtils.getPropertyValue(scatterGather, "gatherEndpoint"));
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherEndpoint"), instanceOf(EventDrivenConsumer.class));
		assertTrue(TestUtils.getPropertyValue(scatterGather, "gatherEndpoint.running", Boolean.class));
		assertEquals(100L, TestUtils.getPropertyValue(scatterGather, "gatherTimeout"));

		assertTrue(this.beanFactory.containsBean("myGatherer"));
		Object gatherer = this.beanFactory.getBean("myGatherer");
		assertSame(gatherer, TestUtils.getPropertyValue(scatterGather, "gatherer"));
		assertSame(this.beanFactory.getBean("messageStore"), TestUtils.getPropertyValue(gatherer, "messageStore"));
		assertSame(gatherer, TestUtils.getPropertyValue(scatterGather, "gatherEndpoint.handler"));

		assertTrue(this.beanFactory.containsBean("myScatterer"));
		Object scatterer = this.beanFactory.getBean("myScatterer");
		assertTrue(TestUtils.getPropertyValue(scatterer, "applySequence", Boolean.class));

		Collection<RecipientListRouter.Recipient> recipients = TestUtils.getPropertyValue(scatterer, "recipients",
				Collection.class);
		assertEquals(1, recipients.size());
		assertSame(this.beanFactory.getBean("distributionChannel"), recipients.iterator().next().getChannel());

		Object scatterChannel = TestUtils.getPropertyValue(scatterGather, "scatterChannel");
		assertThat(scatterChannel, instanceOf(FixedSubscriberChannel.class));
		assertSame(scatterer, TestUtils.getPropertyValue(scatterChannel, "handler"));

		assertTrue(gatherEndpoint.isRunning());
		((Lifecycle) scatterGather).stop();
		assertFalse(((Lifecycle) scatterGather).isRunning());
		assertFalse(gatherEndpoint.isRunning());

	}

}

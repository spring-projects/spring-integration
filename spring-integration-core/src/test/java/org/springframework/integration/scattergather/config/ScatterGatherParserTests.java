/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.scattergather.config;

import java.util.Collection;

import org.junit.jupiter.api.Test;

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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Abdul Zaheer
 * @author Glenn Renfro
 *
 * @since 4.1
 */
@SpringJUnitConfig
@DirtiesContext
public class ScatterGatherParserTests {

	@Autowired
	private BeanFactory beanFactory;

	@Test
	public void testAuction() {
		MessageHandler scatterGather = this.beanFactory.getBean("scatterGather1.handler", MessageHandler.class);
		assertThat(scatterGather).isInstanceOf(ScatterGatherHandler.class);
		assertThat(TestUtils.<Object>getPropertyValue(scatterGather, "scatterChannel"))
				.isSameAs(this.beanFactory.getBean("scatterChannel"));
		assertThat(this.beanFactory.containsBean("scatterGather1.gatherer")).isTrue();
		AggregatingMessageHandler gatherer =
				this.beanFactory.getBean("scatterGather1.gatherer", AggregatingMessageHandler.class);
		assertThat(TestUtils.<AggregatingMessageHandler>getPropertyValue(scatterGather, "gatherer")).isSameAs(gatherer);

		Object reaper = this.beanFactory.getBean("reaper");
		assertThat(TestUtils.<Object>getPropertyValue(reaper, "messageGroupStore"))
				.isSameAs(gatherer.getMessageStore());
		assertThat(TestUtils.<Boolean>getPropertyValue(scatterGather, "requiresReply")).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDistribution() {
		MessageHandler scatterGather = this.beanFactory.getBean("scatterGather2.handler", MessageHandler.class);
		assertThat(TestUtils.<Object>getPropertyValue(scatterGather, "gatherChannel"))
				.isSameAs(this.beanFactory.getBean("gatherChannel"));
		Lifecycle gatherEndpoint = TestUtils.getPropertyValue(scatterGather, "gatherEndpoint");
		assertThat(TestUtils.<Object>getPropertyValue(scatterGather, "gatherEndpoint")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(scatterGather, "gatherEndpoint"))
				.isInstanceOf(EventDrivenConsumer.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(scatterGather, "gatherEndpoint.running")).isTrue();
		assertThat(TestUtils.<Long>getPropertyValue(scatterGather, "gatherTimeout")).isEqualTo(100L);

		assertThat(this.beanFactory.containsBean("myGatherer")).isTrue();
		Object gatherer = this.beanFactory.getBean("myGatherer");
		assertThat(TestUtils.<Object>getPropertyValue(scatterGather, "gatherer")).isSameAs(gatherer);
		assertThat(TestUtils.<Object>getPropertyValue(gatherer, "messageStore"))
				.isSameAs(this.beanFactory.getBean("messageStore"));
		assertThat(TestUtils.<Object>getPropertyValue(scatterGather, "gatherEndpoint.handler"))
				.isSameAs(gatherer);

		assertThat(this.beanFactory.containsBean("myScatterer")).isTrue();
		Object scatterer = this.beanFactory.getBean("myScatterer");
		assertThat(TestUtils.<Boolean>getPropertyValue(scatterer, "applySequence")).isTrue();

		Collection<RecipientListRouter.Recipient> recipients =
				TestUtils.getPropertyValue(scatterer, "recipients");
		assertThat(recipients.size()).isEqualTo(1);
		assertThat(recipients.iterator().next().getChannel()).isSameAs(
				this.beanFactory.getBean("distributionChannel"));

		Object scatterChannel = TestUtils.getPropertyValue(scatterGather, "scatterChannel");
		assertThat(scatterChannel).isInstanceOf(FixedSubscriberChannel.class);
		assertThat(TestUtils.<Object>getPropertyValue(scatterChannel, "handler")).isSameAs(scatterer);

		assertThat(gatherEndpoint.isRunning()).isTrue();
		((Lifecycle) scatterGather).stop();
		assertThat(((Lifecycle) scatterGather).isRunning()).isFalse();
		assertThat(gatherEndpoint.isRunning()).isFalse();

	}

}

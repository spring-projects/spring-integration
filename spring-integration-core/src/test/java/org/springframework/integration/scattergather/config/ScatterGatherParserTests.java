/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
		assertThat(TestUtils.getPropertyValue(scatterGather, "scatterChannel"))
				.isSameAs(this.beanFactory.getBean("scatterChannel"));
		assertThat(this.beanFactory.containsBean("scatterGather1.gatherer")).isTrue();
		AggregatingMessageHandler gatherer =
				this.beanFactory.getBean("scatterGather1.gatherer", AggregatingMessageHandler.class);
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherer")).isSameAs(gatherer);

		Object reaper = this.beanFactory.getBean("reaper");
		assertThat(TestUtils.getPropertyValue(reaper, "messageGroupStore")).isSameAs(gatherer.getMessageStore());
		assertThat(TestUtils.getPropertyValue(scatterGather, "requiresReply", Boolean.class)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testDistribution() {
		MessageHandler scatterGather = this.beanFactory.getBean("scatterGather2.handler", MessageHandler.class);
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherChannel"))
				.isSameAs(this.beanFactory.getBean("gatherChannel"));
		Lifecycle gatherEndpoint = TestUtils.getPropertyValue(scatterGather, "gatherEndpoint", Lifecycle.class);
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherEndpoint")).isNotNull();
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherEndpoint")).isInstanceOf(EventDrivenConsumer.class);
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherEndpoint.running", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherTimeout")).isEqualTo(100L);

		assertThat(this.beanFactory.containsBean("myGatherer")).isTrue();
		Object gatherer = this.beanFactory.getBean("myGatherer");
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherer")).isSameAs(gatherer);
		assertThat(TestUtils.getPropertyValue(gatherer, "messageStore"))
				.isSameAs(this.beanFactory.getBean("messageStore"));
		assertThat(TestUtils.getPropertyValue(scatterGather, "gatherEndpoint.handler")).isSameAs(gatherer);

		assertThat(this.beanFactory.containsBean("myScatterer")).isTrue();
		Object scatterer = this.beanFactory.getBean("myScatterer");
		assertThat(TestUtils.getPropertyValue(scatterer, "applySequence", Boolean.class)).isTrue();

		Collection<RecipientListRouter.Recipient> recipients = TestUtils.getPropertyValue(scatterer, "recipients",
				Collection.class);
		assertThat(recipients.size()).isEqualTo(1);
		assertThat(recipients.iterator().next().getChannel()).isSameAs(this.beanFactory.getBean("distributionChannel"));

		Object scatterChannel = TestUtils.getPropertyValue(scatterGather, "scatterChannel");
		assertThat(scatterChannel).isInstanceOf(FixedSubscriberChannel.class);
		assertThat(TestUtils.getPropertyValue(scatterChannel, "handler")).isSameAs(scatterer);

		assertThat(gatherEndpoint.isRunning()).isTrue();
		((Lifecycle) scatterGather).stop();
		assertThat(((Lifecycle) scatterGather).isRunning()).isFalse();
		assertThat(gatherEndpoint.isRunning()).isFalse();

	}

}

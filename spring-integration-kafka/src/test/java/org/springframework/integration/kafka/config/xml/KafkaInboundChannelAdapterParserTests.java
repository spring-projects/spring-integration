/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class KafkaInboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("adapter1.source")
	private KafkaMessageSource<?, ?> source1;

	@Autowired
	@Qualifier("adapter2.source")
	private KafkaMessageSource<?, ?> source2;

	@Autowired
	private ApplicationContext context;

	@Test
	public void testProps() {
		assertThat(TestUtils.getPropertyValue(this.source1, "consumerProperties.topics")).isEqualTo(new String[] {"topic1"});
		assertThat(TestUtils.getPropertyValue(this.source1, "consumerFactory"))
				.isSameAs(this.context.getBean("consumerFactory"));
		assertThat(TestUtils.getPropertyValue(this.source1, "ackCallbackFactory"))
				.isSameAs(this.context.getBean("ackFactory"));
		assertThat(TestUtils.getPropertyValue(this.source1, "consumerProperties.clientId")).isEqualTo("client");
		assertThat(TestUtils.getPropertyValue(this.source1, "consumerProperties.groupId")).isEqualTo("group");
		assertThat(TestUtils.getPropertyValue(this.source1, "messageConverter"))
				.isSameAs(this.context.getBean("converter"));
		assertThat(TestUtils.getPropertyValue(this.source1, "payloadType")).isEqualTo(String.class);
		assertThat(TestUtils.getPropertyValue(this.source1, "rawMessageHeader", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.source1, "consumerProperties.consumerRebalanceListener"))
				.isSameAs(this.context.getBean("rebal"));

		assertThat(TestUtils.getPropertyValue(this.source2, "consumerProperties.topics")).isEqualTo(new String[] {"topic1", "topic2"});
		DefaultKafkaConsumerFactory<?, ?> cf = TestUtils.getPropertyValue(this.source2, "consumerFactory",
				DefaultKafkaConsumerFactory.class);
		assertThat(cf).isSameAs(this.context.getBean("multiFetchConsumerFactory"));
		assertThat(cf.getConfigurationProperties().get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG)).isEqualTo("10");
	}

}

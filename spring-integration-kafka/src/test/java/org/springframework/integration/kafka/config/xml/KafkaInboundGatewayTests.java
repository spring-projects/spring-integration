/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.kafka.inbound.KafkaInboundGateway;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class KafkaInboundGatewayTests {

	@Autowired
	private KafkaInboundGateway<?, ?, ?> gateway1;

	@Autowired
	private ApplicationContext context;

	@Autowired
	SmartLifecycleRoleController roleController;

	@Test
	public void testProps() {
		assertThat(this.gateway1.isAutoStartup()).isFalse();
		assertThat(this.gateway1.isRunning()).isFalse();
		assertThat(this.gateway1.getPhase()).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(this.gateway1, "requestChannelName")).isEqualTo("nullChannel");
		assertThat(TestUtils.getPropertyValue(this.gateway1, "replyChannelName")).isEqualTo("errorChannel");
		KafkaMessageListenerContainer<?, ?> container =
				TestUtils.getPropertyValue(this.gateway1, "messageListenerContainer",
						KafkaMessageListenerContainer.class);
		assertThat(container).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.gateway1, "listener.fallbackType"))
				.isEqualTo(String.class);
		assertThat(TestUtils.getPropertyValue(this.gateway1, "errorMessageStrategy"))
				.isSameAs(this.context.getBean("ems"));
		assertThat(TestUtils.getPropertyValue(this.gateway1, "retryTemplate"))
				.isSameAs(this.context.getBean("retryTemplate"));
		assertThat(TestUtils.getPropertyValue(this.gateway1, "recoveryCallback"))
				.isSameAs(this.context.getBean("recoveryCallback"));
		assertThat(TestUtils.getPropertyValue(this.gateway1, "onPartitionsAssignedSeekCallback"))
				.isSameAs(this.context.getBean("onPartitionsAssignedSeekCallback"));
		assertThat(TestUtils.getPropertyValue(this.gateway1, "messagingTemplate.sendTimeout")).isEqualTo(5000L);
		assertThat(TestUtils.getPropertyValue(this.gateway1, "messagingTemplate.receiveTimeout")).isEqualTo(43L);
		assertThat(TestUtils.getPropertyValue(this.gateway1, "bindSourceRecord", Boolean.class)).isTrue();
		assertThat(this.roleController.getRoles()).contains("testRole");
		assertThat(this.roleController.getEndpointsRunningStatus("testRole")).containsEntry("gateway1", false);
	}

}

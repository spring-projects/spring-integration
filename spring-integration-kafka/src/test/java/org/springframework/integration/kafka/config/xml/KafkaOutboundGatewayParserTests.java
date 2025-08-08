/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Tom van den Berge
 *
 * @since 5.4
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class KafkaOutboundGatewayParserTests {

	@Autowired
	@Qualifier("allProps.handler")
	KafkaProducerMessageHandler<?, ?> messageHandler;

	@Autowired
	private ApplicationContext context;

	@Test
	public void testProps() {
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "errorMessageStrategy")).isInstanceOf(EMS.class);
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "kafkaTemplate"))
				.isSameAs(this.context.getBean("template"));
		assertThat(this.messageHandler.getOrder()).isEqualTo(23);
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "topicExpression.expression")).isEqualTo("'topic'");
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "messageKeyExpression.expression"))
				.isEqualTo("'key'");
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "partitionIdExpression.expression")).isEqualTo("2");
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "sync", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "sendTimeoutExpression.expression")).isEqualTo("44");
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "timestampExpression.expression"))
				.isEqualTo("T(System).currentTimeMillis()");
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "flushExpression.expression"))
				.isEqualTo("headers['foo']");

		assertThat(TestUtils.getPropertyValue(this.messageHandler, "errorMessageStrategy"))
				.isSameAs(this.context.getBean("ems"));
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "sendFailureChannel"))
				.isSameAs(this.context.getBean("failures"));
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "sendSuccessChannel"))
				.isSameAs(this.context.getBean("successes"));
		assertThat(TestUtils.getPropertyValue(this.messageHandler, "headerMapper"))
				.isSameAs(this.context.getBean("customHeaderMapper"));
	}

	@Component
	public static class EMS extends DefaultErrorMessageStrategy {

		@SuppressWarnings("rawtypes")
		@Bean
		public KafkaTemplate template() {
			ProducerFactory pf = mock(ProducerFactory.class);
			Map<String, Object> props = new HashMap<>();
			given(pf.getConfigurationProperties()).willReturn(props);
			KafkaTemplate template = mock(KafkaTemplate.class);
			given(template.getProducerFactory()).willReturn(pf);
			return template;
		}

	}

}

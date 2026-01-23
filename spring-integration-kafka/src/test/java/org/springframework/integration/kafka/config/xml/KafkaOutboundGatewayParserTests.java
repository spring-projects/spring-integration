/*
 * Copyright 2019-present the original author or authors.
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
 * @author Glenn Renfro
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
		assertThat(TestUtils.<Object>getPropertyValue(this.messageHandler, "errorMessageStrategy"))
				.isInstanceOf(EMS.class);
		assertThat(TestUtils.<Object>getPropertyValue(this.messageHandler, "kafkaTemplate"))
				.isSameAs(this.context.getBean("template"));
		assertThat(this.messageHandler.getOrder()).isEqualTo(23);
		assertThat(TestUtils.<String>getPropertyValue(this.messageHandler, "topicExpression.expression"))
				.isEqualTo("'topic'");
		assertThat(TestUtils.<String>getPropertyValue(this.messageHandler, "messageKeyExpression.expression"))
				.isEqualTo("'key'");
		assertThat(TestUtils.<String>getPropertyValue(this.messageHandler, "partitionIdExpression.expression"))
				.isEqualTo("2");
		assertThat(TestUtils.<Boolean>getPropertyValue(this.messageHandler, "sync")).isTrue();
		assertThat(TestUtils.<String>getPropertyValue(this.messageHandler, "sendTimeoutExpression.expression"))
				.isEqualTo("44");
		assertThat(TestUtils.<String>getPropertyValue(this.messageHandler, "timestampExpression.expression"))
				.isEqualTo("T(System).currentTimeMillis()");
		assertThat(TestUtils.<String>getPropertyValue(this.messageHandler, "flushExpression.expression"))
				.isEqualTo("headers['foo']");

		assertThat(TestUtils.<Object>getPropertyValue(this.messageHandler, "errorMessageStrategy"))
				.isSameAs(this.context.getBean("ems"));
		assertThat(TestUtils.<Object>getPropertyValue(this.messageHandler, "sendFailureChannel"))
				.isSameAs(this.context.getBean("failures"));
		assertThat(TestUtils.<Object>getPropertyValue(this.messageHandler, "sendSuccessChannel"))
				.isSameAs(this.context.getBean("successes"));
		assertThat(TestUtils.<Object>getPropertyValue(this.messageHandler, "headerMapper"))
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

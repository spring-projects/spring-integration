/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.jms.inbound;

import jakarta.jms.Message;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.jms.StubTextMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.MessagingException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 *
 * @since 7.2
 */
class JmsDestinationPollingSourceTests {

	@Test
	void nullPayloadFromConverterThrowsMessageConversionException() throws Exception {
		Message jmsMessage = new StubTextMessage("test");

		MessageConverter converter = mock();

		JmsTemplate jmsTemplate = mock();
		given(jmsTemplate.getMessageConverter()).willReturn(converter);
		given(jmsTemplate.receiveSelected(any())).willReturn(jmsMessage);

		BeanFactory beanFactory = mock();
		String evaluationContextBeanName = IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME;
		given(beanFactory.containsBean(evaluationContextBeanName)).willReturn(true);
		given(beanFactory.getBean(evaluationContextBeanName, StandardEvaluationContext.class))
				.willReturn(new StandardEvaluationContext());

		JmsDestinationPollingSource source = new JmsDestinationPollingSource(jmsTemplate);
		source.setBeanFactory(beanFactory);
		source.afterPropertiesSet();

		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(source::receive)
				.withCauseInstanceOf(MessageConversionException.class);
	}

}

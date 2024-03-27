/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.aop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class PublisherExpressionTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	@BeforeEach
	public void setup() throws Exception {
		this.context.registerSingleton("testChannel", QueueChannel.class);
		IntegrationEvaluationContextFactoryBean factory = new IntegrationEvaluationContextFactoryBean();
		factory.setApplicationContext(this.context);
		factory.afterPropertiesSet();
		EvaluationContext ec = factory.getObject();
		this.context.getBeanFactory()
				.registerSingleton(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME, ec);
		this.context.getBeanFactory().registerSingleton("foo", "foo");
	}

	@AfterEach
	public void tearDown() {
		this.context.close();
	}

	@Test // INT-1139
	public void returnValue() {
		PublisherAnnotationAdvisor advisor = new PublisherAnnotationAdvisor();
		advisor.setBeanFactory(this.context);
		QueueChannel testChannel = this.context.getBean("testChannel", QueueChannel.class);
		advisor.setDefaultChannelName("testChannel");
		ProxyFactory pf = new ProxyFactory(new TestBeanImpl());
		pf.addAdvisor(advisor);
		TestBean proxy = (TestBean) pf.getProxy();
		proxy.test("123");
		Message<?> message = testChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("hellofoo");
		assertThat(message.getHeaders().get("foo")).isEqualTo("123");
	}

	interface TestBean {

		String test(String sku);

	}

	static class TestBeanImpl implements TestBean {

		@Override
		@Publisher
		@Payload("#return + @foo")
		public String test(@Header("foo") String foo) {
			return "hello";
		}

	}

}

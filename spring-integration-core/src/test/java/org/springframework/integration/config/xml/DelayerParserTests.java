/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config.xml;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 1.0.3
 */
@SpringJUnitConfig
@DirtiesContext
public class DelayerParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void defaultScheduler() {
		DelayHandler delayHandler = context.getBean("delayerWithDefaultScheduler.handler", DelayHandler.class);
		assertThat(delayHandler.getOrder()).isEqualTo(99);
		assertThat(delayHandler.getOutputChannel()).isSameAs(this.context.getBean("output"));
		assertThat(TestUtils.<Long>getPropertyValue(delayHandler, "defaultDelay")).isEqualTo(1234L);
		//INT-2243
		assertThat(TestUtils.<Object>getPropertyValue(delayHandler, "delayExpression")).isNotNull();
		assertThat(TestUtils.<Expression>getPropertyValue(delayHandler, "delayExpression").getExpressionString())
				.isEqualTo("headers.foo");
		assertThat(TestUtils.<Long>getPropertyValue(delayHandler, "messagingTemplate.sendTimeout"))
				.isEqualTo(987L);
		assertThat(TestUtils.<Object>getPropertyValue(delayHandler, "taskScheduler")).isNotNull();
	}

	@Test
	public void customScheduler() {
		Object endpoint = context.getBean("delayerWithCustomScheduler");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertThat(handler.getClass()).isEqualTo(DelayHandler.class);
		DelayHandler delayHandler = (DelayHandler) handler;
		assertThat(delayHandler.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
		DirectFieldAccessor accessor = new DirectFieldAccessor(delayHandler);
		assertThat(delayHandler.getOutputChannel()).isSameAs(this.context.getBean("output"));
		assertThat(accessor.getPropertyValue("defaultDelay")).isEqualTo(0L);
		assertThat(accessor.getPropertyValue("taskScheduler")).isEqualTo(context.getBean("testScheduler"));
		assertThat(accessor.getPropertyValue("taskScheduler")).isNotNull();
		assertThat(new DirectFieldAccessor(
				accessor.getPropertyValue("taskScheduler")).getPropertyValue("waitForTasksToCompleteOnShutdown"))
				.isEqualTo(Boolean.TRUE);
	}

	@Test
	public void customMessageStore() {
		Object endpoint = context.getBean("delayerWithCustomMessageStore");
		assertThat(endpoint.getClass()).isEqualTo(EventDrivenConsumer.class);
		Object handler = TestUtils.getPropertyValue(endpoint, "handler");
		assertThat(handler.getClass()).isEqualTo(DelayHandler.class);
		DelayHandler delayHandler = (DelayHandler) handler;
		DirectFieldAccessor accessor = new DirectFieldAccessor(delayHandler);
		assertThat(accessor.getPropertyValue("messageStore")).isEqualTo(context.getBean("testMessageStore"));
	}

	@Test //INT-2649
	public void transactionalSubElement() throws Exception {
		Object endpoint = context.getBean("delayerWithTransactional");
		DelayHandler delayHandler = TestUtils.getPropertyValue(endpoint, "handler");
		List<?> adviceChain = TestUtils.getPropertyValue(delayHandler, "delayedAdviceChain");
		assertThat(adviceChain.size()).isEqualTo(1);
		Object advice = adviceChain.get(0);
		assertThat(advice instanceof TransactionInterceptor).isTrue();
		TransactionAttributeSource transactionAttributeSource =
				((TransactionInterceptor) advice).getTransactionAttributeSource();
		assertThat(transactionAttributeSource instanceof MatchAlwaysTransactionAttributeSource).isTrue();
		Method method = MessageHandler.class.getMethod("handleMessage", Message.class);
		TransactionDefinition definition = transactionAttributeSource.getTransactionAttribute(method, null);
		assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
		assertThat(definition.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
		assertThat(definition.getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
		assertThat(definition.isReadOnly()).isFalse();
	}

	@Test //INT-2649
	public void adviceChainSubElement() {
		Object endpoint = context.getBean("delayerWithAdviceChain");
		DelayHandler delayHandler = TestUtils.getPropertyValue(endpoint, "handler");
		List<?> adviceChain = TestUtils.getPropertyValue(delayHandler, "delayedAdviceChain");
		assertThat(adviceChain.size()).isEqualTo(2);
		assertThat(adviceChain.get(0)).isSameAs(context.getBean("testAdviceBean"));

		Object txAdvice = adviceChain.get(1);
		assertThat(txAdvice.getClass()).isEqualTo(TransactionInterceptor.class);
		TransactionAttributeSource transactionAttributeSource =
				((TransactionInterceptor) txAdvice).getTransactionAttributeSource();
		assertThat(transactionAttributeSource.getClass()).isEqualTo(NameMatchTransactionAttributeSource.class);
		HashMap<?, ?> nameMap = TestUtils.getPropertyValue(transactionAttributeSource, "nameMap");
		assertThat(nameMap.toString()).isEqualTo("{*=PROPAGATION_REQUIRES_NEW,ISOLATION_DEFAULT,readOnly}");
	}

	@Test
	public void testInt2243Expression() {
		DelayHandler delayHandler = context.getBean("delayerWithExpression.handler", DelayHandler.class);
		assertThat(TestUtils.<Expression>getPropertyValue(delayHandler, "delayExpression").getExpressionString())
				.isEqualTo("100");
		assertThat(TestUtils.<Boolean>getPropertyValue(delayHandler, "ignoreExpressionFailures")).isFalse();
	}

	@Test
	public void testInt2243ExpressionSubElement() {
		DelayHandler delayHandler = context.getBean("delayerWithExpressionSubElement.handler", DelayHandler.class);
		assertThat(TestUtils.<Expression>getPropertyValue(delayHandler, "delayExpression").getExpressionString())
				.isEqualTo("headers.timestamp + 1000");
	}

}

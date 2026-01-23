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

package org.springframework.integration.dispatcher;

import java.util.List;
import java.util.concurrent.Callable;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.Advised;
import org.springframework.context.Lifecycle;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.TestTransactionManager;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Andreas Baer
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class PollingTransactionTests {

	@Test
	public void transactionWithCommit() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		MessageChannel input = (MessageChannel) context.getBean("goodInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		assertThat(txManager.getRollbackCount()).isEqualTo(0);
		context.getBean("goodService", Lifecycle.class).start();
		input.send(new GenericMessage<>("test"));
		txManager.waitForCompletion(10000);
		Message<?> message = output.receive(0);
		assertThat(message).isNotNull();
		assertThat(txManager.getCommitCount()).isEqualTo(1);
		assertThat(txManager.getRollbackCount()).isEqualTo(0);
		context.close();
	}

	@Test
	public void transactionWithCommitAndAdvices() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionTests.xml", this.getClass());
		PollingConsumer advisedPoller = context.getBean("advisedSa", PollingConsumer.class);

		List<Advice> adviceChain = TestUtils.getPropertyValue(advisedPoller, "adviceChain");
		assertThat(adviceChain.size()).isEqualTo(4);
		advisedPoller.start();
		Callable<?> pollingTask = TestUtils.getPropertyValue(advisedPoller, "pollingTask");
		assertThat(pollingTask instanceof Advised).as("Poller is not Advised").isTrue();
		Advisor[] advisors = ((Advised) pollingTask).getAdvisors();
		assertThat(advisors.length).isEqualTo(4);

		assertThat(advisors[0].getAdvice()).as("First advisor is not TX").isInstanceOf(TransactionInterceptor.class);
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		MessageChannel input = (MessageChannel) context.getBean("goodInputWithAdvice");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		assertThat(txManager.getRollbackCount()).isEqualTo(0);

		input.send(new GenericMessage<>("test"));
		input.send(new GenericMessage<>("test2"));
		txManager.waitForCompletion(10000);
		Message<?> message = output.receive(0);
		assertThat(message).isNotNull();
		message = output.receive(0);
		assertThat(message).isNotNull();
		assertThat(txManager.getRollbackCount()).isEqualTo(0);
		context.close();
	}

	@Test
	public void transactionWithRollback() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		MessageChannel input = (MessageChannel) context.getBean("badInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		assertThat(txManager.getRollbackCount()).isEqualTo(0);

		context.getBean("badService", Lifecycle.class).start();

		input.send(new GenericMessage<>("test"));
		txManager.waitForCompletion(10000);
		Message<?> message = output.receive(0);
		assertThat(message).isNull();
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		assertThat(txManager.getRollbackCount()).isEqualTo(1);
		context.close();
	}

	@Test
	public void propagationRequired() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationRequiredTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		input.send(new GenericMessage<>("test"));
		Message<?> reply = output.receive(10000);
		assertThat(reply).isNotNull();
		txManager.waitForCompletion(10000);
		assertThat(txManager.getCommitCount()).isEqualTo(1);
		assertThat(txManager.getLastDefinition().getPropagationBehavior()).isEqualTo(Propagation.REQUIRED.value());
		context.close();
	}

	@Test
	public void propagationRequiresNew() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationRequiresNewTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		input.send(new GenericMessage<>("test"));
		Message<?> reply = output.receive(10000);
		assertThat(reply).isNotNull();
		txManager.waitForCompletion(10000);
		assertThat(txManager.getCommitCount()).isEqualTo(1);
		assertThat(txManager.getLastDefinition().getPropagationBehavior()).isEqualTo(Propagation.REQUIRES_NEW.value());
		context.close();
	}

	@Test
	public void propagationSupports() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationSupportsTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		input.send(new GenericMessage<>("test"));
		Message<?> reply = output.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		assertThat(txManager.getLastDefinition()).isNull();
		context.close();
	}

	@Test
	public void propagationNotSupported() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationNotSupportedTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		input.send(new GenericMessage<>("test"));
		Message<?> reply = output.receive(10000);
		assertThat(reply).isNotNull();
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		assertThat(txManager.getLastDefinition()).isNull();
		context.close();
	}

	@Test
	public void propagationMandatory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationMandatoryTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		PollableChannel errorChannel = (PollableChannel) context.getBean("errorChannel");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		input.send(new GenericMessage<>("test"));
		Message<?> errorMessage = errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();
		Object payload = errorMessage.getPayload();
		assertThat(payload.getClass()).isEqualTo(MessagingException.class);
		MessagingException messagingException = (MessagingException) payload;
		assertThat(messagingException.getCause().getClass()).isEqualTo(IllegalTransactionStateException.class);
		assertThat(output.receive(0)).isNull();
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		context.close();
	}

	@Test
	public void commitFailureAndHandlerFailureTest() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionFailureTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManagerBad");
		PollableChannel inputTxFail = (PollableChannel) context.getBean("inputTxFailure");
		PollableChannel inputHandlerFail = (PollableChannel) context.getBean("inputHandlerFailure");
		PollableChannel output = (PollableChannel) context.getBean("output");
		PollableChannel errorChannel = (PollableChannel) context.getBean("errorChannel");
		assertThat(txManager.getCommitCount()).isEqualTo(0);
		inputTxFail.send(new GenericMessage<>("commitFailureTest"));
		Message<?> errorMessage = errorChannel.receive(20000);
		assertThat(errorMessage).isNotNull();
		Object payload = errorMessage.getPayload();
		assertThat(payload.getClass()).isEqualTo(MessagingException.class);
		MessagingException messagingException = (MessagingException) payload;
		assertThat(messagingException.getCause().getClass()).isEqualTo(IllegalTransactionStateException.class);
		assertThat(messagingException.getFailedMessage()).isNotNull();
		assertThat(output.receive(0)).isNotNull();
		assertThat(txManager.getCommitCount()).isEqualTo(0);

		inputHandlerFail.send(new GenericMessage<>("handlerFailureTest"));
		errorMessage = errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();
		payload = errorMessage.getPayload();
		assertThat(payload.getClass()).isEqualTo(MessageHandlingException.class);
		messagingException = (MessageHandlingException) payload;
		assertThat(messagingException.getCause().getClass()).isEqualTo(RuntimeException.class);
		assertThat(messagingException.getFailedMessage()).isNotNull();
		assertThat(output.receive(0)).isNull();
		assertThat(txManager.getCommitCount()).isEqualTo(0);

		context.close();
	}

	public static class SampleAdvice implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			return invocation.proceed();
		}

	}

	public static class SimpleRepeatAdvice implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			((ProxyMethodInvocation) invocation).invocableClone().proceed();

			return invocation.proceed();
		}

	}

	@SuppressWarnings("serial")
	public static class FailingCommitTransactionManager extends TestTransactionManager {

		@Override
		protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
			throw new IllegalTransactionStateException("intentional test commit failure");
		}

	}

}

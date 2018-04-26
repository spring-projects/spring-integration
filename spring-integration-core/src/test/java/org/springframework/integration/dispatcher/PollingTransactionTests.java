/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.dispatcher;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

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

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Andreas Baer
 * @author Artem Bilan
 */
public class PollingTransactionTests {

	@Test
	public void transactionWithCommit() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		MessageChannel input = (MessageChannel) context.getBean("goodInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());
		context.getBean("goodService", Lifecycle.class).start();
		input.send(new GenericMessage<>("test"));
		txManager.waitForCompletion(10000);
		Message<?> message = output.receive(0);
		assertNotNull(message);
		assertEquals(1, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());
		context.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void transactionWithCommitAndAdvices() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionTests.xml", this.getClass());
		PollingConsumer advisedPoller = context.getBean("advisedSa", PollingConsumer.class);

		List<Advice> adviceChain = TestUtils.getPropertyValue(advisedPoller, "adviceChain", List.class);
		assertEquals(4, adviceChain.size());
		advisedPoller.start();
		Runnable poller = TestUtils.getPropertyValue(advisedPoller, "poller", Runnable.class);
		Callable<?> pollingTask = TestUtils.getPropertyValue(poller, "pollingTask", Callable.class);
		assertTrue("Poller is not Advised", pollingTask instanceof Advised);
		Advisor[] advisors = ((Advised) pollingTask).getAdvisors();
		assertEquals(4, advisors.length);

		assertThat("First advisor is not TX", advisors[0].getAdvice(), instanceOf(TransactionInterceptor.class));
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		MessageChannel input = (MessageChannel) context.getBean("goodInputWithAdvice");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());

		input.send(new GenericMessage<>("test"));
		input.send(new GenericMessage<>("test2"));
		txManager.waitForCompletion(10000);
		Message<?> message = output.receive(0);
		assertNotNull(message);
		message = output.receive(0);
		assertNotNull(message);
		assertEquals(0, txManager.getRollbackCount());
		context.close();
	}

	@Test
	public void transactionWithRollback() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		MessageChannel input = (MessageChannel) context.getBean("badInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());

		context.getBean("badService", Lifecycle.class).start();

		input.send(new GenericMessage<>("test"));
		txManager.waitForCompletion(10000);
		Message<?> message = output.receive(0);
		assertNull(message);
		assertEquals(0, txManager.getCommitCount());
		assertEquals(1, txManager.getRollbackCount());
		context.close();
	}

	@Test
	public void propagationRequired() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationRequiredTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		input.send(new GenericMessage<>("test"));
		Message<?> reply = output.receive(10000);
		assertNotNull(reply);
		txManager.waitForCompletion(10000);
		assertEquals(1, txManager.getCommitCount());
		assertEquals(Propagation.REQUIRED.value(), txManager.getLastDefinition().getPropagationBehavior());
		context.close();
	}

	@Test
	public void propagationRequiresNew() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationRequiresNewTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		input.send(new GenericMessage<>("test"));
		Message<?> reply = output.receive(10000);
		assertNotNull(reply);
		txManager.waitForCompletion(10000);
		assertEquals(1, txManager.getCommitCount());
		assertEquals(Propagation.REQUIRES_NEW.value(), txManager.getLastDefinition().getPropagationBehavior());
		context.close();
	}

	@Test
	public void propagationSupports() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationSupportsTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		input.send(new GenericMessage<>("test"));
		Message<?> reply = output.receive(10000);
		assertNotNull(reply);
		assertEquals(0, txManager.getCommitCount());
		assertNull(txManager.getLastDefinition());
		context.close();
	}

	@Test
	public void propagationNotSupported() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationNotSupportedTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		input.send(new GenericMessage<>("test"));
		Message<?> reply = output.receive(10000);
		assertNotNull(reply);
		assertEquals(0, txManager.getCommitCount());
		assertNull(txManager.getLastDefinition());
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
		assertEquals(0, txManager.getCommitCount());
		input.send(new GenericMessage<>("test"));
		Message<?> errorMessage = errorChannel.receive(10000);
		assertNotNull(errorMessage);
		Object payload = errorMessage.getPayload();
		assertEquals(MessagingException.class, payload.getClass());
		MessagingException messagingException = (MessagingException) payload;
		assertEquals(IllegalTransactionStateException.class, messagingException.getCause().getClass());
		assertNull(output.receive(0));
		assertEquals(0, txManager.getCommitCount());
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
		assertEquals(0, txManager.getCommitCount());
		inputTxFail.send(new GenericMessage<>("commitFailureTest"));
		Message<?> errorMessage = errorChannel.receive(20000);
		assertNotNull(errorMessage);
		Object payload = errorMessage.getPayload();
		assertEquals(MessagingException.class, payload.getClass());
		MessagingException messagingException = (MessagingException) payload;
		assertEquals(IllegalTransactionStateException.class, messagingException.getCause().getClass());
		assertNotNull(messagingException.getFailedMessage());
		assertNotNull(output.receive(0));
		assertEquals(0, txManager.getCommitCount());

		inputHandlerFail.send(new GenericMessage<>("handlerFailureTest"));
		errorMessage = errorChannel.receive(10000);
		assertNotNull(errorMessage);
		payload = errorMessage.getPayload();
		assertEquals(MessageHandlingException.class, payload.getClass());
		messagingException = (MessageHandlingException) payload;
		assertEquals(RuntimeException.class, messagingException.getCause().getClass());
		assertNotNull(messagingException.getFailedMessage());
		assertNull(output.receive(0));
		assertEquals(0, txManager.getCommitCount());

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

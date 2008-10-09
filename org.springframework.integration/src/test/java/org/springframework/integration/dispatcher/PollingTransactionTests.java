/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.util.TestTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;

/**
 * @author Mark Fisher
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
		input.send(new StringMessage("test"));
		txManager.waitForCompletion(1000);
		Message<?> message = output.receive(0);
		assertNotNull(message);		
		assertEquals(1, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());
		context.stop();
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
		input.send(new StringMessage("test"));
		txManager.waitForCompletion(1000);
		Message<?> message = output.receive(0);
		assertNull(message);
		assertEquals(0, txManager.getCommitCount());
		assertEquals(1, txManager.getRollbackCount());
		context.stop();
	}

	@Test
	public void propagationRequired() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationRequiredTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		input.send(new StringMessage("test"));
		Message<?> reply = output.receive(3000);
		assertNotNull(reply);
		txManager.waitForCompletion(3000);
		assertEquals(1, txManager.getCommitCount());
		assertEquals(Propagation.REQUIRED.value(), txManager.getLastDefinition().getPropagationBehavior());
		context.stop();
	}

	@Test
	public void propagationRequiresNew() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationRequiresNewTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		input.send(new StringMessage("test"));
		Message<?> reply = output.receive(3000);
		assertNotNull(reply);
		txManager.waitForCompletion(3000);
		assertEquals(1, txManager.getCommitCount());
		assertEquals(Propagation.REQUIRES_NEW.value(), txManager.getLastDefinition().getPropagationBehavior());
		context.stop();
	}

	@Test
	public void propagationSupports() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationSupportsTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		input.send(new StringMessage("test"));
		Message<?> reply = output.receive(3000);
		assertNotNull(reply);
		assertEquals(0, txManager.getCommitCount());
		assertNull(txManager.getLastDefinition());
		context.stop();
	}

	@Test
	public void propagationNotSupported() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationNotSupportedTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		input.send(new StringMessage("test"));
		Message<?> reply = output.receive(3000);
		assertNotNull(reply);
		assertEquals(0, txManager.getCommitCount());
		assertNull(txManager.getLastDefinition());
		context.stop();
	}

	@Test
	public void propagationMandatory() throws Throwable {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"propagationMandatoryTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		PollableChannel input = (PollableChannel) context.getBean("input");
		PollableChannel output = (PollableChannel) context.getBean("output");
		PollableChannel errorChannel = (PollableChannel) context.getBean("errorChannel");
		assertEquals(0, txManager.getCommitCount());
		input.send(new StringMessage("test"));
		Message<?> errorMessage = errorChannel.receive(3000);
		assertNotNull(errorMessage);
		Object payload = errorMessage.getPayload();
		assertEquals(IllegalTransactionStateException.class, payload.getClass());
		assertNull(output.receive(0));
		assertEquals(0, txManager.getCommitCount());
		context.stop();
	}

}

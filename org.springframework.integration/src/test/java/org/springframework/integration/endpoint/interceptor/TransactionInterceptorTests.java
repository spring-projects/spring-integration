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

package org.springframework.integration.endpoint.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.StringMessage;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Mark Fisher
 */
public class TransactionInterceptorTests {

	@Test
	public void testTransactionInterceptorWithCommit() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionInterceptorTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		MessageChannel input = (MessageChannel) context.getBean("goodInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());
		input.send(new StringMessage("test"));
		txManager.waitForCompletion(1000);
		Message<?> message = output.receive(500);
		assertNotNull(message);		
		assertEquals(1, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());		
	}

	@Test
	public void testTransactionInterceptorWithRollback() throws InterruptedException {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionInterceptorTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		MessageChannel input = (MessageChannel) context.getBean("badInput");
		PollableChannel output = (PollableChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());
		input.send(new StringMessage("test"));
		txManager.waitForCompletion(1000);
		Message<?> message = output.receive(500);
		assertNull(message);
		assertEquals(0, txManager.getCommitCount());
		assertEquals(1, txManager.getRollbackCount());		
	}

	@Test
	public void testPropagationRequired() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionInterceptorPropagationTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		final MessageEndpoint endpoint = (MessageEndpoint) context.getBean("required");
		assertEquals(0, txManager.getCommitCount());
		endpoint.send(new StringMessage("test"));
		assertEquals(1, txManager.getCommitCount());
		TestTransactionManager outerTxManager = new TestTransactionManager();
		TransactionTemplate txTemplate = new TransactionTemplate(outerTxManager);
		txTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return endpoint.send(new StringMessage("test"));
			}
		});
		assertEquals(1, outerTxManager.getCommitCount());
		assertEquals(2, txManager.getCommitCount());
		assertEquals(Propagation.REQUIRED.value(), txManager.getLastDefinition().getPropagationBehavior());
	}

	@Test
	public void testPropagationRequiresNew() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionInterceptorPropagationTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		final MessageEndpoint endpoint = (MessageEndpoint) context.getBean("requiresNew");
		assertEquals(0, txManager.getCommitCount());
		endpoint.send(new StringMessage("test"));
		assertEquals(1, txManager.getCommitCount());
		TestTransactionManager outerTxManager = new TestTransactionManager();
		TransactionTemplate txTemplate = new TransactionTemplate(outerTxManager);
		txTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return endpoint.send(new StringMessage("test"));
			}
		});
		assertEquals(1, outerTxManager.getCommitCount());
		assertEquals(2, txManager.getCommitCount());
		assertEquals(Propagation.REQUIRES_NEW.value(), txManager.getLastDefinition().getPropagationBehavior());
	}

	@Test
	public void testPropagationSupports() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionInterceptorPropagationTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		final MessageEndpoint endpoint = (MessageEndpoint) context.getBean("supports");
		assertEquals(0, txManager.getCommitCount());
		endpoint.send(new StringMessage("test"));
		assertEquals(0, txManager.getCommitCount());
		TestTransactionManager outerTxManager = new TestTransactionManager();
		TransactionTemplate txTemplate = new TransactionTemplate(outerTxManager);
		txTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return endpoint.send(new StringMessage("test"));
			}
		});
		assertEquals(0, txManager.getCommitCount());
		assertEquals(1, outerTxManager.getCommitCount());
	}

	@Test
	public void testPropagationNotSupported() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionInterceptorPropagationTests.xml", this.getClass());
		TestTransactionManager txManager = (TestTransactionManager) context.getBean("txManager");
		final MessageEndpoint endpoint = (MessageEndpoint) context.getBean("notSupported");
		assertEquals(0, txManager.getCommitCount());
		endpoint.send(new StringMessage("test"));
		assertEquals(0, txManager.getCommitCount());
		TestTransactionManager outerTxManager = new TestTransactionManager();
		TransactionTemplate txTemplate = new TransactionTemplate(outerTxManager);
		txTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				return endpoint.send(new StringMessage("test"));
			}
		});
		assertEquals(0, txManager.getCommitCount());
		assertEquals(1, outerTxManager.getCommitCount());
	}

	@Test(expected = IllegalTransactionStateException.class)
	public void testPropagationMandatoryCalledWithoutTransaction() throws Throwable {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"transactionInterceptorPropagationTests.xml", this.getClass());
		final MessageEndpoint endpoint = (MessageEndpoint) context.getBean("mandatory");
		try {
			endpoint.send(new StringMessage("test"));
		}
		catch (MessageHandlingException e) {
			throw e.getCause();
		}
	}

}

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
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

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
		MessageChannel output = (MessageChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());
		input.send(new StringMessage("test"));
		txManager.waitForCompletion(1000);
		Message<?> message = output.receive(0);
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
		MessageChannel output = (MessageChannel) context.getBean("output");
		assertEquals(0, txManager.getCommitCount());
		assertEquals(0, txManager.getRollbackCount());
		input.send(new StringMessage("test"));
		txManager.waitForCompletion(1000);
		Message<?> message = output.receive(0);
		assertNull(message);
		assertEquals(0, txManager.getCommitCount());
		assertEquals(1, txManager.getRollbackCount());		
	}

}

/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.MessageSourceResourceHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public class PseudoTransactionalMessageSourceTests {

	@Test
	public void testCommit() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
				new ExpressionEvaluatingTransactionSynchronizationProcessor();
		PollableChannel queueChannel = new QueueChannel();
		syncProcessor.setAfterCommitChannel(queueChannel);
		syncProcessor.setAfterCommitExpression(new SpelExpressionParser().parseExpression("#resource.attributes['baz']"));

		DefaultTransactionSynchronizationFactory syncFactory =
				new DefaultTransactionSynchronizationFactory(syncProcessor);

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setSource(new MessageSource<String>() {

			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<String>("foo");
				((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this)).getAttributes()
						.put("baz", "qux");
				return message;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		adapter.doPoll();
		TransactionSynchronizationUtils.triggerAfterCommit();
		Message<?> commitMessage = queueChannel.receive(1000);
		assertEquals("qux", commitMessage.getPayload());
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	public void testRollback() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
				new ExpressionEvaluatingTransactionSynchronizationProcessor();
		PollableChannel queueChannel = new QueueChannel();
		syncProcessor.setAfterRollbackChannel(queueChannel);
		syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#resource.attributes['baz']"));

		DefaultTransactionSynchronizationFactory syncFactory =
				new DefaultTransactionSynchronizationFactory(syncProcessor);

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setSource(new MessageSource<String>() {

			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<String>("foo");
				((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this)).getAttributes()
						.put("baz", "qux");
				return message;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		adapter.doPoll();
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}


	public class Bar {
		public String getValue() {
			return "bar";
		}
	}
}

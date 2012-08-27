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
import static org.junit.Assert.assertNotNull;

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
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.transaction.support.TransactionTemplate;

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
		syncProcessor.setBeforeCommitExpression(new SpelExpressionParser().parseExpression("#bix"));
		syncProcessor.setBeforeCommitChannel(queueChannel);
		syncProcessor.setAfterCommitChannel(queueChannel);
		syncProcessor.setAfterCommitExpression(new SpelExpressionParser().parseExpression("#baz"));

		DefaultTransactionSynchronizationFactory syncFactory =
				new DefaultTransactionSynchronizationFactory(syncProcessor);

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setSource(new MessageSource<String>() {

			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<String>("foo");
				((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("baz", "qux");
				((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("bix", "qox");
				return message;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		adapter.doPoll();
		TransactionSynchronizationUtils.triggerBeforeCommit(false);
		TransactionSynchronizationUtils.triggerAfterCommit();
		Message<?> beforeCommitMessage = queueChannel.receive(1000);
		assertNotNull(beforeCommitMessage);
		assertEquals("qox", beforeCommitMessage.getPayload());
		Message<?> afterCommitMessage = queueChannel.receive(1000);
		assertNotNull(afterCommitMessage);
		assertEquals("qux", afterCommitMessage.getPayload());
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
		syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

		DefaultTransactionSynchronizationFactory syncFactory =
				new DefaultTransactionSynchronizationFactory(syncProcessor);

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setSource(new MessageSource<String>() {

			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<String>("foo");
				((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("baz", "qux");
				return message;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		adapter.doPoll();
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertNotNull(rollbackMessage);
		assertEquals("qux", rollbackMessage.getPayload());
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	public void testCommitWithManager() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		transactionTemplate.execute(new TransactionCallback<Object>() {

			public Object doInTransaction(TransactionStatus status) {
				SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
				ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
						new ExpressionEvaluatingTransactionSynchronizationProcessor();
				syncProcessor.setBeforeCommitExpression(new SpelExpressionParser().parseExpression("#bix"));
				syncProcessor.setBeforeCommitChannel(queueChannel);
				syncProcessor.setAfterCommitChannel(queueChannel);
				syncProcessor.setAfterCommitExpression(new SpelExpressionParser().parseExpression("#baz"));

				DefaultTransactionSynchronizationFactory syncFactory =
						new DefaultTransactionSynchronizationFactory(syncProcessor);

				adapter.setTransactionSynchronizationFactory(syncFactory);

				QueueChannel outputChannel = new QueueChannel();
				adapter.setOutputChannel(outputChannel);
				adapter.setSource(new MessageSource<String>() {

					public Message<String> receive() {
						GenericMessage<String> message = new GenericMessage<String>("foo");
						((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("baz", "qux");
						((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this)).addAttribute("bix", "qox");
						return message;
					}
				});

				adapter.doPoll();
				return null;
			}
		});
		Message<?> beforeCommitMessage = queueChannel.receive(1000);
		assertNotNull(beforeCommitMessage);
		assertEquals("qox", beforeCommitMessage.getPayload());
		Message<?> afterCommitMessage = queueChannel.receive(1000);
		assertNotNull(afterCommitMessage);
		assertEquals("qux", afterCommitMessage.getPayload());
	}

	@Test
	public void testRollbackWithManager() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		try {
			transactionTemplate.execute(new TransactionCallback<Object>() {

				public Object doInTransaction(TransactionStatus status) {

					SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
					ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor = new ExpressionEvaluatingTransactionSynchronizationProcessor();
					syncProcessor.setAfterRollbackChannel(queueChannel);
					syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

					DefaultTransactionSynchronizationFactory syncFactory = new DefaultTransactionSynchronizationFactory(
							syncProcessor);

					adapter.setTransactionSynchronizationFactory(syncFactory);

					QueueChannel outputChannel = new QueueChannel();
					adapter.setOutputChannel(outputChannel);
					adapter.setSource(new MessageSource<String>() {

						public Message<String> receive() {
							GenericMessage<String> message = new GenericMessage<String>("foo");
							((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this))
									.addAttribute("baz", "qux");
							return message;
						}
					});

					adapter.doPoll();
					throw new RuntimeException("Force rollback");
				}
			});
		}
		catch (Exception e) {
			assertEquals("Force rollback", e.getMessage());
		}
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertNotNull(rollbackMessage);
		assertEquals("qux", rollbackMessage.getPayload());
	}

	@Test
	public void testRollbackWithManagerUsingStatus() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		transactionTemplate.execute(new TransactionCallback<Object>() {

			public Object doInTransaction(TransactionStatus status) {

				SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
				ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor = new ExpressionEvaluatingTransactionSynchronizationProcessor();
				syncProcessor.setAfterRollbackChannel(queueChannel);
				syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

				DefaultTransactionSynchronizationFactory syncFactory = new DefaultTransactionSynchronizationFactory(
						syncProcessor);

				adapter.setTransactionSynchronizationFactory(syncFactory);

				QueueChannel outputChannel = new QueueChannel();
				adapter.setOutputChannel(outputChannel);
				adapter.setSource(new MessageSource<String>() {

					public Message<String> receive() {
						GenericMessage<String> message = new GenericMessage<String>("foo");
						((MessageSourceResourceHolder) TransactionSynchronizationManager.getResource(this))
								.addAttribute("baz", "qux");
						return message;
					}
				});

				adapter.doPoll();
				status.setRollbackOnly();
				return null;
			}
		});
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertNotNull(rollbackMessage);
		assertEquals("qux", rollbackMessage.getPayload());
	}

	public class Bar {
		public String getValue() {
			return "bar";
		}
	}
}

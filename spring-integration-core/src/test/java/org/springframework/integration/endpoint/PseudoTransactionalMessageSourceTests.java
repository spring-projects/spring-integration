/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.transaction.DefaultTransactionSynchronizationFactory;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.PseudoTransactionManager;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.integration.transaction.TransactionSynchronizationFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class PseudoTransactionalMessageSourceTests {

	@Test
	public void testCommit() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
				new ExpressionEvaluatingTransactionSynchronizationProcessor();
		syncProcessor.setBeanFactory(mock(BeanFactory.class));
		PollableChannel queueChannel = new QueueChannel();
		syncProcessor.setBeforeCommitExpression(new SpelExpressionParser().parseExpression("#bix"));
		syncProcessor.setBeforeCommitChannel(queueChannel);
		syncProcessor.setAfterCommitExpression(new SpelExpressionParser().parseExpression("#baz"));

		DefaultTransactionSynchronizationFactory syncFactory =
				new DefaultTransactionSynchronizationFactory(syncProcessor);

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setSource(new MessageSource<String>() {

			@Override
			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<>("foo");
				IntegrationResourceHolder holder =
						(IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this);
				holder.addAttribute("baz", "qux");
				holder.addAttribute("bix", "qox");
				return message;
			}
		});

		QueueChannel afterCommitChannel = new QueueChannel();
		syncProcessor.setAfterCommitChannel(afterCommitChannel);
		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerBeforeCommit(false);
		TransactionSynchronizationUtils.triggerAfterCommit();
		Message<?> beforeCommitMessage = queueChannel.receive(1000);
		assertThat(beforeCommitMessage).isNotNull();
		assertThat(beforeCommitMessage.getPayload()).isEqualTo("qox");

		assertThat(afterCommitChannel.receive(1000))
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo("qux");

		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	public void testTransactionSynchronizationFactoryBean() {
		ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(TestTxSyncConfiguration.class);

		TransactionSynchronizationFactory syncFactory = ctx.getBean(TransactionSynchronizationFactory.class);

		PollableChannel queueChannel = ctx.getBean("outputChannel", PollableChannel.class);

		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);
		adapter.setSource(new MessageSource<String>() {

			@Override
			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<>("foo");
				IntegrationResourceHolder holder =
						(IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this);
				holder.addAttribute("baz", "qux");
				holder.addAttribute("bix", "qox");
				return message;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerBeforeCommit(false);
		TransactionSynchronizationUtils.triggerAfterCommit();
		Message<?> beforeCommitMessage = queueChannel.receive(1000);
		assertThat(beforeCommitMessage).isNotNull();
		assertThat(beforeCommitMessage.getPayload()).isEqualTo("qox");
		Message<?> afterCommitMessage = queueChannel.receive(1000);
		assertThat(afterCommitMessage).isNotNull();
		assertThat(afterCommitMessage.getPayload()).isEqualTo("qux");
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
		ctx.close();
	}


	@Test
	public void testRollback() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
				new ExpressionEvaluatingTransactionSynchronizationProcessor();
		syncProcessor.setBeanFactory(mock(BeanFactory.class));
		PollableChannel queueChannel = new QueueChannel();
		syncProcessor.setAfterRollbackChannel(queueChannel);
		syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

		DefaultTransactionSynchronizationFactory syncFactory =
				new DefaultTransactionSynchronizationFactory(syncProcessor);

		adapter.setTransactionSynchronizationFactory(syncFactory);

		QueueChannel outputChannel = new QueueChannel();
		adapter.setOutputChannel(outputChannel);

		final Message<?> testMessage = new GenericMessage<>("foo");

		adapter.setSource(new MessageSource<String>() {

			@Override
			public Message<String> receive() {
				GenericMessage<String> message = new GenericMessage<>("foo");
				((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this))
						.addAttribute("baz", testMessage);
				return message;
			}
		});

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertThat(rollbackMessage).isNotNull();
		assertThat(rollbackMessage).isSameAs(testMessage);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	public void testCommitWithManager() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		transactionTemplate.execute(status -> {
			SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
			ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
					new ExpressionEvaluatingTransactionSynchronizationProcessor();
			syncProcessor.setBeanFactory(mock(BeanFactory.class));
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

				@Override
				public Message<String> receive() {
					GenericMessage<String> message = new GenericMessage<>("foo");
					IntegrationResourceHolder holder =
							(IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this);
					holder.addAttribute("baz", "qux");
					holder.addAttribute("bix", "qox");
					return message;
				}
			});

			doPoll(adapter);
			return null;
		});
		Message<?> beforeCommitMessage = queueChannel.receive(1000);
		assertThat(beforeCommitMessage).isNotNull();
		assertThat(beforeCommitMessage.getPayload()).isEqualTo("qox");
		Message<?> afterCommitMessage = queueChannel.receive(1000);
		assertThat(afterCommitMessage).isNotNull();
		assertThat(afterCommitMessage.getPayload()).isEqualTo("qux");
	}

	@Test
	public void testRollbackWithManager() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		try {
			transactionTemplate.execute(status -> {

				SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
				ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
						new ExpressionEvaluatingTransactionSynchronizationProcessor();
				syncProcessor.setBeanFactory(mock(BeanFactory.class));
				syncProcessor.setAfterRollbackChannel(queueChannel);
				syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

				DefaultTransactionSynchronizationFactory syncFactory =
						new DefaultTransactionSynchronizationFactory(syncProcessor);

				adapter.setTransactionSynchronizationFactory(syncFactory);

				QueueChannel outputChannel = new QueueChannel();
				adapter.setOutputChannel(outputChannel);
				adapter.setSource(new MessageSource<String>() {

					@Override
					public Message<String> receive() {
						GenericMessage<String> message = new GenericMessage<>("foo");
						((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this))
								.addAttribute("baz", "qux");
						return message;
					}
				});

				doPoll(adapter);
				throw new RuntimeException("Force rollback");
			});
		}
		catch (Exception e) {
			assertThat(e.getMessage()).isEqualTo("Force rollback");
		}
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertThat(rollbackMessage).isNotNull();
		assertThat(rollbackMessage.getPayload()).isEqualTo("qux");
	}

	@Test
	public void testRollbackWithManagerUsingStatus() {
		final PollableChannel queueChannel = new QueueChannel();
		TransactionTemplate transactionTemplate = new TransactionTemplate(new PseudoTransactionManager());
		transactionTemplate.execute(status -> {

			SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
			ExpressionEvaluatingTransactionSynchronizationProcessor syncProcessor =
					new ExpressionEvaluatingTransactionSynchronizationProcessor();
			syncProcessor.setBeanFactory(mock(BeanFactory.class));
			syncProcessor.setAfterRollbackChannel(queueChannel);
			syncProcessor.setAfterRollbackExpression(new SpelExpressionParser().parseExpression("#baz"));

			DefaultTransactionSynchronizationFactory syncFactory =
					new DefaultTransactionSynchronizationFactory(syncProcessor);

			adapter.setTransactionSynchronizationFactory(syncFactory);

			QueueChannel outputChannel = new QueueChannel();
			adapter.setOutputChannel(outputChannel);
			adapter.setSource(new MessageSource<String>() {

				@Override
				public Message<String> receive() {
					GenericMessage<String> message = new GenericMessage<>("foo");
					((IntegrationResourceHolder) TransactionSynchronizationManager.getResource(this))
							.addAttribute("baz", "qux");
					return message;
				}
			});

			doPoll(adapter);
			status.setRollbackOnly();
			return null;
		});
		Message<?> rollbackMessage = queueChannel.receive(1000);
		assertThat(rollbackMessage).isNotNull();
		assertThat(rollbackMessage.getPayload()).isEqualTo("qux");
	}

	@Test
	public void testInt2777UnboundResourceAfterTransactionComplete() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();

		adapter.setSource(() -> null);

		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationManager.setActualTransactionActive(false);

		// Before INT-2777 this test was failed here
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationManager.setActualTransactionActive(false);
	}

	@Test
	public void testInt2777CustomTransactionSynchronizationFactoryWithoutDealWithIntegrationResourceHolder() {
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();

		final AtomicInteger txSyncCounter = new AtomicInteger();

		TransactionSynchronizationFactory syncFactory = key -> new TransactionSynchronization() {

			@Override
			public void afterCompletion(int status) {
				txSyncCounter.incrementAndGet();
			}

		};

		adapter.setTransactionSynchronizationFactory(syncFactory);
		adapter.setSource(() -> null);

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
		assertThat(txSyncCounter.get()).isEqualTo(1);

		TransactionSynchronizationManager.initSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(true);
		doPoll(adapter);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		TransactionSynchronizationManager.clearSynchronization();
		TransactionSynchronizationManager.setActualTransactionActive(false);
		assertThat(txSyncCounter.get()).isEqualTo(2);
	}

	protected void doPoll(SourcePollingChannelAdapter adapter) {
		assertThatNoException()
				.isThrownBy(() -> {
					Method method = AbstractPollingEndpoint.class.getDeclaredMethod("doPoll");
					method.setAccessible(true);
					method.invoke(adapter);
				});
	}

	public class Bar {

		public String getValue() {
			return "bar";
		}

	}

	@Configuration
	@EnableIntegration
	public static class TestTxSyncConfiguration {

		@Bean
		public MessageChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		public TransactionSynchronizationFactoryBean txSync() {
			return new TransactionSynchronizationFactoryBean()
					.beforeCommit("#bix")
					.beforeCommit(outputChannel())
					.afterCommit("#baz", outputChannel());
		}

	}

}

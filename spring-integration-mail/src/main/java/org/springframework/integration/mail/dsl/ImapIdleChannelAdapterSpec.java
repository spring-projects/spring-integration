/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.mail.dsl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.mail.Authenticator;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.aopalliance.aop.Advice;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.SearchTermStrategy;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.PropertiesBuilder;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} for a {@link ImapIdleChannelAdapter}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class ImapIdleChannelAdapterSpec
		extends MessageProducerSpec<ImapIdleChannelAdapterSpec, ImapIdleChannelAdapter>
		implements ComponentsRegistration {

	protected final ImapMailReceiver receiver; // NOSONAR - final

	protected final Map<Object, String> componentsToRegister = new LinkedHashMap<>();  // NOSONAR - final

	private final List<Advice> adviceChain = new LinkedList<>();

	protected final boolean externalReceiver; // NOSONAR

	private boolean sessionProvided;

	protected ImapIdleChannelAdapterSpec(ImapMailReceiver receiver) {
		this(receiver, false);
	}

	protected ImapIdleChannelAdapterSpec(ImapMailReceiver receiver, boolean externalReceiver) {
		super(new ImapIdleChannelAdapter(receiver));
		this.target.setAdviceChain(this.adviceChain);
		this.receiver = receiver;
		this.componentsToRegister.put(receiver, receiver.getComponentName());
		this.externalReceiver = externalReceiver;
	}

	/**
	 * Configure a SpEL expression to select messages. The root object for the expression
	 * evaluation is a {@link javax.mail.internet.MimeMessage} which should return a boolean
	 * result (true means select the message).
	 * @param selectorExpression the selectorExpression.
	 * @return the spec.
	 */
	public ImapIdleChannelAdapterSpec selectorExpression(String selectorExpression) {
		return selectorExpression(PARSER.parseExpression(selectorExpression));
	}

	/**
	 * Configure an {@link Expression} to select messages. The root object for the expression
	 * evaluation is a {@link javax.mail.internet.MimeMessage} which should return a boolean
	 * result (true means select the message).
	 * @param selectorExpression the selectorExpression.
	 * @return the spec.
	 */
	public ImapIdleChannelAdapterSpec selectorExpression(Expression selectorExpression) {
		assertReceiver();
		this.receiver.setSelectorExpression(selectorExpression);
		return this;
	}

	private void assertReceiver() {
		Assert.state(!this.externalReceiver,
				() -> "An external 'receiver' [" + this.receiver + "] can't be modified.");
	}

	/**
	 * Configure a {@link Function} to select messages. The argument for the function
	 * is a {@link javax.mail.internet.MimeMessage}; {@code apply} returns a boolean
	 * result (true means select the message).
	 * @param selectorFunction the selectorFunction.
	 * @return the spec.
	 * @see FunctionExpression
	 */
	public ImapIdleChannelAdapterSpec selector(Function<MimeMessage, Boolean> selectorFunction) {
		return selectorExpression(new FunctionExpression<>(selectorFunction));
	}

	/**
	 * A Java Mail {@link Session} to use.
	 * @param session the session.
	 * @return the spec.
	 * @see ImapMailReceiver#setSession(Session)
	 */
	public ImapIdleChannelAdapterSpec session(Session session) {
		assertReceiver();
		this.receiver.setSession(session);
		this.sessionProvided = true;
		return this;
	}

	/**
	 * @param javaMailProperties the javaMailProperties.
	 * @return the spec.
	 * @see ImapMailReceiver#setJavaMailProperties(Properties)
	 */
	public ImapIdleChannelAdapterSpec javaMailProperties(Properties javaMailProperties) {
		assertReceiver();
		assertSession();
		this.receiver.setJavaMailProperties(javaMailProperties);
		return this;
	}

	private void assertSession() {
		Assert.state(!this.sessionProvided, "Neither 'javaMailProperties' nor 'javaMailAuthenticator' "
				+ "references are allowed when a 'session' reference has been provided.");
	}

	/**
	 * Configure the {@code javaMailProperties} by invoking a {@link Consumer} callback which
	 * is invoked with a {@link PropertiesBuilder}.
	 * @param configurer the configurer.
	 * @return the spec.
	 * @see ImapMailReceiver#setJavaMailProperties(Properties)
	 */
	public ImapIdleChannelAdapterSpec javaMailProperties(Consumer<PropertiesBuilder> configurer) {
		PropertiesBuilder properties = new PropertiesBuilder();
		configurer.accept(properties);
		return javaMailProperties(properties.get());
	}

	/**
	 * @param javaMailAuthenticator the javaMailAuthenticator.
	 * @return the spec.
	 * @see ImapMailReceiver#setJavaMailAuthenticator(Authenticator)
	 */
	public ImapIdleChannelAdapterSpec javaMailAuthenticator(Authenticator javaMailAuthenticator) {
		assertReceiver();
		assertSession();
		this.receiver.setJavaMailAuthenticator(javaMailAuthenticator);
		return this;
	}

	/**
	 * @param maxFetchSize the maxFetchSize.
	 * @return the spec.
	 * @see ImapMailReceiver#setMaxFetchSize(int)
	 */
	public ImapIdleChannelAdapterSpec maxFetchSize(int maxFetchSize) {
		assertReceiver();
		this.receiver.setMaxFetchSize(maxFetchSize);
		return this;
	}

	/**
	 * @param shouldDeleteMessages the shouldDeleteMessages.
	 * @return the spec.
	 * @see ImapMailReceiver#setShouldDeleteMessages(boolean)
	 */
	public ImapIdleChannelAdapterSpec shouldDeleteMessages(boolean shouldDeleteMessages) {
		assertReceiver();
		this.receiver.setShouldDeleteMessages(shouldDeleteMessages);
		return this;
	}

	/**
	 * @param searchTermStrategy the searchTermStrategy.
	 * @return the spec.
	 * @see ImapMailReceiver#setSearchTermStrategy(SearchTermStrategy)
	 */
	public ImapIdleChannelAdapterSpec searchTermStrategy(SearchTermStrategy searchTermStrategy) {
		assertReceiver();
		this.receiver.setSearchTermStrategy(searchTermStrategy);
		return this;
	}

	/**
	 * @param shouldMarkMessagesAsRead the shouldMarkMessagesAsRead.
	 * @return the spec.
	 * @see ImapMailReceiver#setShouldMarkMessagesAsRead(Boolean)
	 */
	public ImapIdleChannelAdapterSpec shouldMarkMessagesAsRead(boolean shouldMarkMessagesAsRead) {
		assertReceiver();
		this.receiver.setShouldMarkMessagesAsRead(shouldMarkMessagesAsRead);
		return this;
	}

	/**
	 * Set the name of the flag to use to flag messages when the server does
	 * not support \Recent but supports user flags;
	 * default {@value ImapMailReceiver#DEFAULT_SI_USER_FLAG}.
	 * @param userFlag the flag.
	 * @return the spec.
	 * @see ImapMailReceiver#setUserFlag(String)
	 */
	public ImapIdleChannelAdapterSpec userFlag(String userFlag) {
		assertReceiver();
		this.receiver.setUserFlag(userFlag);
		return _this();
	}

	/**
	 * Set the header mapper; if a header mapper is not provided, the message payload is
	 * a {@link MimeMessage}, when provided, the headers are mapped and the payload is
	 * the {@link MimeMessage} content.
	 * @param headerMapper the header mapper.
	 * @return the spec.
	 * @see ImapMailReceiver#setUserFlag(String)
	 * @see #embeddedPartsAsBytes(boolean)
	 */
	public ImapIdleChannelAdapterSpec headerMapper(HeaderMapper<MimeMessage> headerMapper) {
		assertReceiver();
		this.receiver.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * When a header mapper is provided determine whether an embedded
	 * {@link javax.mail.Part} (e.g {@link javax.mail.Message} or
	 * {@link javax.mail.Multipart} content is rendered as a byte[] in the payload.
	 * Otherwise, leave as a {@link javax.mail.Part}. These objects are not suitable for
	 * downstream serialization. Default: true.
	 * <p>
	 * This has no effect if there is no header mapper, in that case the payload is the
	 * {@link MimeMessage}.
	 * @param embeddedPartsAsBytes the embeddedPartsAsBytes to set.
	 * @return the spec.
	 * @see #headerMapper(HeaderMapper)
	 */
	public ImapIdleChannelAdapterSpec embeddedPartsAsBytes(boolean embeddedPartsAsBytes) {
		assertReceiver();
		this.receiver.setEmbeddedPartsAsBytes(embeddedPartsAsBytes);
		return _this();
	}

	/**
	 * When configured to {@code false}, the folder is not closed automatically after a fetch.
	 * It is the target application's responsibility to close it using the
	 * {@link org.springframework.integration.IntegrationMessageHeaderAccessor#CLOSEABLE_RESOURCE} header
	 * from the message produced by this channel adapter.
	 * @param autoCloseFolder set to {@code false} to keep folder opened.
	 * @return the spec.
	 * @since 5.2
	 * @see ImapMailReceiver#setAutoCloseFolder(boolean)
	 */
	public ImapIdleChannelAdapterSpec autoCloseFolder(boolean autoCloseFolder) {
		assertReceiver();
		this.receiver.setAutoCloseFolder(autoCloseFolder);
		return _this();
	}

	/**
	 * Configure a {@link TransactionSynchronizationFactory}. Usually used to synchronize
	 * message deletion with some external transaction manager.
	 * @param transactionSynchronizationFactory the transactionSynchronizationFactory.
	 * @return the spec.
	 */
	public ImapIdleChannelAdapterSpec transactionSynchronizationFactory(
			TransactionSynchronizationFactory transactionSynchronizationFactory) {

		this.target.setTransactionSynchronizationFactory(transactionSynchronizationFactory);
		return this;
	}

	/**
	 * Configure a chain of {@link Advice} objects for message delivery, applied to
	 * the downstream flow.
	 * @param adviceChain the advice chain.
	 * @return the spec.
	 */
	public ImapIdleChannelAdapterSpec adviceChain(Advice... adviceChain) {
		this.adviceChain.addAll(Arrays.asList(adviceChain));
		return this;
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with the provided
	 * {@link TransactionManager} and default
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
	 * for the downstream flow.
	 * @param transactionManager the {@link TransactionManager} to use.
	 * @return the spec.
	 * @since 5.2.5
	 */
	public ImapIdleChannelAdapterSpec transactional(TransactionManager transactionManager) {
		return transactional(new TransactionInterceptorBuilder(false)
				.transactionManager(transactionManager)
				.build());
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} for the
	 * downstream flow.
	 * @param transactionInterceptor the {@link TransactionInterceptor} to use.
	 * @return the spec.
	 * @see TransactionInterceptorBuilder
	 */
	public ImapIdleChannelAdapterSpec transactional(TransactionInterceptor transactionInterceptor) {
		return adviceChain(transactionInterceptor);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with default
	 * {@code PlatformTransactionManager} and
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute} for the
	 * downstream flow.
	 * @return the spec.
	 */
	public ImapIdleChannelAdapterSpec transactional() {
		TransactionInterceptor transactionInterceptor = new TransactionInterceptorBuilder(false).build();
		this.componentsToRegister.put(transactionInterceptor, null);
		return transactional(transactionInterceptor);
	}

	/**
	 * Specify a task executor to be used to send messages to the downstream flow.
	 * @param sendingTaskExecutor the sendingTaskExecutor.
	 * @return the spec.
	 * @see ImapIdleChannelAdapter#setSendingTaskExecutor(Executor)
	 */
	public ImapIdleChannelAdapterSpec sendingTaskExecutor(Executor sendingTaskExecutor) {
		this.target.setSendingTaskExecutor(sendingTaskExecutor);
		return this;
	}

	/**
	 * @param shouldReconnectAutomatically the shouldReconnectAutomatically.
	 * @return the spec.
	 * @see ImapIdleChannelAdapter#setShouldReconnectAutomatically(boolean)
	 */
	public ImapIdleChannelAdapterSpec shouldReconnectAutomatically(boolean shouldReconnectAutomatically) {
		this.target.setShouldReconnectAutomatically(shouldReconnectAutomatically);
		return this;
	}

	/**
	 * How often to recycle the idle task (in case of a silently dropped connection).
	 * Seconds; default 120 (2 minutes).
	 * @param interval the interval.
	 * @return the spec.
	 * @see ImapMailReceiver#setCancelIdleInterval(long)
	 * @since 5.2
	 */
	public ImapIdleChannelAdapterSpec cancelIdleInterval(long interval) {
		assertReceiver();
		this.receiver.setCancelIdleInterval(interval);
		return this;
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return this.componentsToRegister;
	}

}

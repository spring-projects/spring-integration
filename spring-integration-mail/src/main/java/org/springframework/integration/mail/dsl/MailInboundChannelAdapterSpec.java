/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.mail.dsl;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.mail.AbstractMailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.PropertiesBuilder;
import org.springframework.util.Assert;

/**
 * A {@link MessageSourceSpec} for a {@link MailReceivingMessageSource}.
 *
 *
 * @param <S> the target {@link MailInboundChannelAdapterSpec} implementation type.
 * @param <R> the target {@link AbstractMailReceiver} implementation type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 5.0
 */
public abstract class
		MailInboundChannelAdapterSpec<S extends MailInboundChannelAdapterSpec<S, R>, R extends AbstractMailReceiver>
		extends MessageSourceSpec<S, MailReceivingMessageSource>
		implements ComponentsRegistration {

	protected final R receiver;

	protected final boolean externalReceiver;

	private boolean sessionProvided;

	protected MailInboundChannelAdapterSpec(R receiver) {
		this(receiver, false);
	}

	protected MailInboundChannelAdapterSpec(R receiver, boolean externalReceiver) {
		this.receiver = receiver;
		this.externalReceiver = externalReceiver;
	}

	/**
	 * Configure a SpEL expression to select messages. The root object for the expression
	 * evaluation is a {@link javax.mail.internet.MimeMessage} which should return a boolean
	 * result (true means select the message).
	 * @param selectorExpression the selectorExpression.
	 * @return the spec.
	 */
	public S selectorExpression(String selectorExpression) {
		assertReceiver();
		this.receiver.setSelectorExpression(PARSER.parseExpression(selectorExpression));
		return _this();
	}

	protected void assertReceiver() {
		Assert.state(!this.externalReceiver, "An external 'receiver' [" + this.receiver + "] can't be modified.");
	}

	/**
	 * Configure a SpEL expression to select messages. The root object for the expression
	 * evaluation is a {@link javax.mail.internet.MimeMessage} which should return a boolean
	 * result (true means select the message).
	 * @param selectorExpression the selectorExpression.
	 * @return the spec.
	 */
	public S selectorExpression(Expression selectorExpression) {
		assertReceiver();
		this.receiver.setSelectorExpression(selectorExpression);
		return _this();
	}

	/**
	 * Configure a {@link Function} to select messages. The argument for the function
	 * is a {@link javax.mail.internet.MimeMessage}; {@code apply} returns a boolean
	 * result (true means select the message).
	 * @param selectorFunction the selectorFunction.
	 * @return the spec.
	 * @see FunctionExpression
	 */
	public S selector(Function<MimeMessage, Boolean> selectorFunction) {
		assertReceiver();
		this.receiver.setSelectorExpression(new FunctionExpression<MimeMessage>(selectorFunction));
		return _this();
	}

	/**
	 * Provide the Java Mail {@link Session} to use.
	 * @param session the session.
	 * @return the spec.
	 * @see AbstractMailReceiver#setSession(Session)
	 */
	public S session(Session session) {
		assertReceiver();
		this.receiver.setSession(session);
		this.sessionProvided = true;
		return _this();
	}

	/**
	 * The Java Mail properties.
	 * @param javaMailProperties the javaMailProperties.
	 * @return the spec.
	 * @see AbstractMailReceiver#setJavaMailProperties(Properties)
	 */
	public S javaMailProperties(Properties javaMailProperties) {
		assertReceiver();
		assertSession();
		this.receiver.setJavaMailProperties(javaMailProperties);
		return _this();
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
	 * @see AbstractMailReceiver#setJavaMailProperties(Properties)
	 */
	public S javaMailProperties(Consumer<PropertiesBuilder> configurer) {
		PropertiesBuilder properties = new PropertiesBuilder();
		configurer.accept(properties);
		return javaMailProperties(properties.get());
	}

	/**
	 * The Java Mail {@link Authenticator}.
	 * @param javaMailAuthenticator the javaMailAuthenticator.
	 * @return the spec.
	 * @see AbstractMailReceiver#setJavaMailAuthenticator(Authenticator)
	 */
	public S javaMailAuthenticator(Authenticator javaMailAuthenticator) {
		assertSession();
		assertReceiver();
		this.receiver.setJavaMailAuthenticator(javaMailAuthenticator);
		return _this();
	}

	/**
	 * The maximum for fetch size.
	 * @param maxFetchSize the maxFetchSize.
	 * @return the spec.
	 * @see AbstractMailReceiver#setMaxFetchSize(int)
	 */
	public S maxFetchSize(int maxFetchSize) {
		assertReceiver();
		this.receiver.setMaxFetchSize(maxFetchSize);
		return _this();
	}

	/**
	 * A flag to specify if messages should be deleted after receive.
	 * @param shouldDeleteMessages the shouldDeleteMessages.
	 * @return the spec.
	 * @see AbstractMailReceiver#setShouldDeleteMessages(boolean)
	 */
	public S shouldDeleteMessages(boolean shouldDeleteMessages) {
		assertReceiver();
		this.receiver.setShouldDeleteMessages(shouldDeleteMessages);
		return _this();
	}

	/**
	 * Set the name of the flag to use to flag messages when the server does
	 * not support \Recent but supports user flags;
	 * default {@value AbstractMailReceiver#DEFAULT_SI_USER_FLAG}.
	 * @param userFlag the flag.
	 * @return the spec.
	 * @see AbstractMailReceiver#setUserFlag(String)
	 */
	public S userFlag(String userFlag) {
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
	 * @see AbstractMailReceiver#setUserFlag(String)
	 * @see #embeddedPartsAsBytes(boolean)
	 */
	public S headerMapper(HeaderMapper<MimeMessage> headerMapper) {
		assertReceiver();
		this.receiver.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * When a header mapper is provided determine whether an embedded {@link Part} (e.g
	 * {@link Message} or {@link javax.mail.Multipart} content is rendered as a byte[] in the
	 * payload. Otherwise, leave as a {@link Part}. These objects are not suitable for
	 * downstream serialization. Default: true.
	 * <p>This has no effect if there is no header mapper, in that case the payload is the
	 * {@link MimeMessage}.
	 * @param embeddedPartsAsBytes the embeddedPartsAsBytes to set.
	 * @return the spec.
	 * @see #headerMapper(HeaderMapper)
	 */
	public S embeddedPartsAsBytes(boolean embeddedPartsAsBytes) {
		assertReceiver();
		this.receiver.setEmbeddedPartsAsBytes(embeddedPartsAsBytes);
		return _this();
	}

	/**
	 * Determine how the content is rendered.
	 * @param simpleContent true for simple content.
	 * @return the spec.
	 * @see AbstractMailReceiver#setSimpleContent(boolean)
	 */
	public S simpleContent(boolean simpleContent) {
		assertReceiver();
		this.receiver.setSimpleContent(simpleContent);
		return _this();
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		return Collections.<Object>singletonList(this.receiver);
	}

	@Override
	public MailReceivingMessageSource doGet() {
		return new MailReceivingMessageSource(this.receiver);
	}

}

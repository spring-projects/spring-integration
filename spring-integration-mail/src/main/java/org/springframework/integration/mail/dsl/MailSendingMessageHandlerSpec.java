/*
 * Copyright 2014-2021 the original author or authors.
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

import java.util.Properties;
import java.util.function.Consumer;

import jakarta.activation.FileTypeMap;

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.integration.support.PropertiesBuilder;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MailSendingMessageHandlerSpec
		extends MessageHandlerSpec<MailSendingMessageHandlerSpec, MailSendingMessageHandler> {

	protected final JavaMailSenderImpl sender = new JavaMailSenderImpl(); // NOSONAR - final

	protected MailSendingMessageHandlerSpec(@Nullable String host) {
		this.sender.setHost(host);
		this.target = new MailSendingMessageHandler(this.sender);
	}

	/**
	 * Set the javamail properties.
	 * @param javaMailProperties the properties.
	 * @return the spec.
	 * @see JavaMailSenderImpl#setJavaMailProperties(Properties)
	 */
	public MailSendingMessageHandlerSpec javaMailProperties(Properties javaMailProperties) {
		this.sender.setJavaMailProperties(javaMailProperties);
		return this;
	}

	/**
	 * Set a {@link Consumer} that will be invoked with a {@link PropertiesBuilder}; most often
	 * used with a lambda expression.
	 * @param propertiesConfigurer the consumer.
	 * @return the spec.
	 */
	public MailSendingMessageHandlerSpec javaMailProperties(Consumer<PropertiesBuilder> propertiesConfigurer) {
		PropertiesBuilder properties = new PropertiesBuilder();
		propertiesConfigurer.accept(properties);
		return javaMailProperties(properties.get());
	}

	/**
	 * Set the protocol.
	 * @param protocol the protocol.
	 * @return the spec.
	 * @see JavaMailSenderImpl#setProtocol(String)
	 */
	public MailSendingMessageHandlerSpec protocol(@Nullable String protocol) {
		this.sender.setProtocol(protocol);
		return this;
	}

	/**
	 * Set the port.
	 * @param port the port.
	 * @return the spec.
	 * @see JavaMailSenderImpl#setPort(int)
	 */
	public MailSendingMessageHandlerSpec port(int port) {
		this.sender.setPort(port);
		return this;
	}

	/**
	 * Set the credentials.
	 * @param username the user name.
	 * @param password the password.
	 * @return the spec.
	 * @see JavaMailSenderImpl#setUsername(String)
	 * @see JavaMailSenderImpl#setPassword(String)
	 */
	public MailSendingMessageHandlerSpec credentials(@Nullable String username, @Nullable String password) {
		this.sender.setUsername(username);
		this.sender.setPassword(password);
		return this;
	}

	/**
	 * Set the mail user password.
	 * A convenient method when {@code username} is provided in the Java mail properties.
	 * @param password the password.
	 * @return the spec.
	 * @since 5.1.3
	 * @see JavaMailSenderImpl#setPassword(String)
	 * @see #javaMailProperties(Properties)
	 */
	public MailSendingMessageHandlerSpec password(@Nullable String password) {
		this.sender.setPassword(password);
		return this;
	}

	/**
	 * Set the default encoding.
	 * @param defaultEncoding the default encoding.
	 * @return the spec.
	 * @see JavaMailSenderImpl#setDefaultEncoding(String)
	 */
	public MailSendingMessageHandlerSpec defaultEncoding(@Nullable String defaultEncoding) {
		this.sender.setDefaultEncoding(defaultEncoding);
		return this;
	}

	/**
	 * Set the default type map.
	 * @param defaultFileTypeMap the default type map.
	 * @return the spec.
	 * @see JavaMailSenderImpl#setDefaultFileTypeMap(FileTypeMap)
	 */
	public MailSendingMessageHandlerSpec defaultFileTypeMap(@Nullable FileTypeMap defaultFileTypeMap) {
		this.sender.setDefaultFileTypeMap(defaultFileTypeMap);
		return this;
	}

}

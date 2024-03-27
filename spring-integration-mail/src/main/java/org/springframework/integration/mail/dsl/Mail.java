/*
 * Copyright 2014-2024 the original author or authors.
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

import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.integration.mail.transformer.MailToStringTransformer;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailSender;

/**
 * The factory for Spring Integration Mail components.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class Mail {

	/**
	 * A {@link MailSendingMessageHandlerSpec} factory.
	 * Note: the Java Mail properties must be provided with the particular host.
	 * @return the {@link MailSendingMessageHandlerSpec} instance.
	 * @since 5.1.3
	 * @see MailSendingMessageHandlerSpec#javaMailProperties
	 */
	public static MailSendingMessageHandlerSpec outboundAdapter() {
		return new MailSendingMessageHandlerSpec(null);
	}

	/**
	 * A {@link MailSendingMessageHandlerSpec} factory based on provide {@code host}.
	 * @param host the mail host to connect to.
	 * @return the {@link MailSendingMessageHandlerSpec} instance.
	 */
	public static MailSendingMessageHandlerSpec outboundAdapter(@Nullable String host) {
		return new MailSendingMessageHandlerSpec(host);
	}

	/**
	 * A convenient factory method to produce {@link MailSendingMessageHandler}
	 * based on provided {@link MailSender}.
	 * @param mailSender the {@link MailSender} to use mail sending operations.
	 * @return the {@link MailSendingMessageHandler} instance.
	 * @since 5.1.3
	 */
	public static MailSendingMessageHandler outboundAdapter(MailSender mailSender) {
		return new MailSendingMessageHandler(mailSender);
	}

	/**
	 * A {@link Pop3MailInboundChannelAdapterSpec} factory using a default
	 * {@link Pop3MailReceiver}.
	 * @return the {@link Pop3MailInboundChannelAdapterSpec} instance.
	 */
	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter() {
		return new Pop3MailInboundChannelAdapterSpec();
	}

	/**
	 * A {@link Pop3MailInboundChannelAdapterSpec} factory based on the provided
	 * {@link Pop3MailReceiver}.
	 * @param pop3MailReceiver the {@link Pop3MailReceiver} to use.
	 * @return the {@link Pop3MailInboundChannelAdapterSpec} instance.
	 */
	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter(Pop3MailReceiver pop3MailReceiver) {
		return new Pop3MailInboundChannelAdapterSpec(pop3MailReceiver);
	}

	/**
	 * A {@link Pop3MailInboundChannelAdapterSpec} factory based on the provided url.
	 * @param url the pop3 url
	 * @return the {@link Pop3MailInboundChannelAdapterSpec} instance.
	 */
	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter(String url) {
		return new Pop3MailInboundChannelAdapterSpec(url);
	}

	/**
	 * A {@link Pop3MailInboundChannelAdapterSpec} factory based on the provided host,
	 * user, password and the default port.
	 * @param host the host.
	 * @param username the user.
	 * @param password the password.
	 * @return the {@link Pop3MailInboundChannelAdapterSpec} instance.
	 */
	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter(String host, String username, String password) {
		return pop3InboundAdapter(host, -1, username, password);
	}

	/**
	 * A {@link Pop3MailInboundChannelAdapterSpec} factory based on the provided host,
	 * port, user, and password.
	 * @param host the host.
	 * @param port the port.
	 * @param username the user.
	 * @param password the password.
	 * @return the {@link Pop3MailInboundChannelAdapterSpec} instance.
	 */
	public static Pop3MailInboundChannelAdapterSpec pop3InboundAdapter(String host, int port, String username,
			String password) {

		return new Pop3MailInboundChannelAdapterSpec(host, port, username, password);
	}

	/**
	 * An {@link ImapMailInboundChannelAdapterSpec} factory using a default {@link ImapMailReceiver}.
	 * @return the {@link ImapMailInboundChannelAdapterSpec} instance.
	 */
	public static ImapMailInboundChannelAdapterSpec imapInboundAdapter() {
		return new ImapMailInboundChannelAdapterSpec();
	}

	/**
	 * An {@link ImapMailInboundChannelAdapterSpec} factory based on the provided {@link ImapMailReceiver}.
	 * @param imapMailReceiver the {@link ImapMailReceiver} to use.
	 * @return the {@link ImapMailInboundChannelAdapterSpec} instance.
	 */
	public static ImapMailInboundChannelAdapterSpec imapInboundAdapter(ImapMailReceiver imapMailReceiver) {
		return new ImapMailInboundChannelAdapterSpec(imapMailReceiver);
	}

	/**
	 * A {@link ImapMailInboundChannelAdapterSpec} factory based on the provided url.
	 * @param url the imap url
	 * @return the {@link ImapMailInboundChannelAdapterSpec} instance.
	 */
	public static ImapMailInboundChannelAdapterSpec imapInboundAdapter(String url) {
		return new ImapMailInboundChannelAdapterSpec(url);
	}

	/**
	 * An {@link ImapIdleChannelAdapterSpec} factory using a default {@link ImapMailReceiver}.
	 * @return the {@link ImapIdleChannelAdapterSpec} instance.
	 */
	public static ImapIdleChannelAdapterSpec imapIdleAdapter() {
		return new ImapIdleChannelAdapterSpec(new ImapMailReceiver());
	}

	/**
	 * A {@link ImapIdleChannelAdapterSpec} factory based on the provided url.
	 * @param url the imap url
	 * @return the {@link ImapIdleChannelAdapterSpec} instance.
	 */
	public static ImapIdleChannelAdapterSpec imapIdleAdapter(String url) {
		return new ImapIdleChannelAdapterSpec(new ImapMailReceiver(url));
	}

	/**
	 * An {@link ImapIdleChannelAdapterSpec} factory based on the provided {@link ImapMailReceiver}.
	 * @param imapMailReceiver the {@link ImapMailReceiver} to use.
	 * @return the {@link ImapIdleChannelAdapterSpec} instance.
	 */
	public static ImapIdleChannelAdapterSpec imapIdleAdapter(ImapMailReceiver imapMailReceiver) {
		return new ImapIdleChannelAdapterSpec(imapMailReceiver, true);
	}

	/**
	 * A {@link MailHeadersBuilder} factory.
	 * @return the factory.
	 */
	public static MailHeadersBuilder headers() {
		return new MailHeadersBuilder();
	}

	/**
	 * A {@link MailToStringTransformer} factory.
	 * @return the transformer.
	 */
	public static MailToStringTransformer toStringTransformer() {
		return toStringTransformer(null);
	}

	/**
	 * A {@link MailToStringTransformer} factory.
	 * @param charset the charset to use when the default is not appropriate.
	 * @return the transformer.
	 */
	public static MailToStringTransformer toStringTransformer(@Nullable String charset) {
		MailToStringTransformer transformer = new MailToStringTransformer();
		if (charset != null) {
			transformer.setCharset(charset);
		}
		return transformer;
	}

	private Mail() {
	}

}

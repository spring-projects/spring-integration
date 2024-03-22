/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats.util;

import java.util.HashMap;
import java.util.Map;

import io.nats.client.impl.Headers;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * NATS Utils
 *
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 */
public final class NatsUtils {

	public static final String IPG_CONTEXT_MESSAGE_KEY = "ipgContextMessageKey";

	public static final String MAIL_REQUEST_TYPE = "mailRequestType";

	private NatsUtils() {
		super();
	}

	/**
	 * Method to generate NATS Message Header {@link Headers} using the information extracted from
	 * spring integration message
	 *
	 * @param message Spring integration message from the the information is extracted
	 * @return NATS Message Headers
	 */
	@NonNull
	public static Headers populateNatsMessageHeaders(final @NonNull Message<?> message) {
		final MessageHeaders messageHeaders = message.getHeaders();
		final Headers headers = new Headers();
		final String ipgContextMessageKeyVal =
				messageHeaders.get(IPG_CONTEXT_MESSAGE_KEY, String.class);
		if (ipgContextMessageKeyVal != null) {
			headers.add(IPG_CONTEXT_MESSAGE_KEY, ipgContextMessageKeyVal);
		}
		final String mailRequestType = messageHeaders.get(MAIL_REQUEST_TYPE, String.class);
		if (mailRequestType != null) {
			headers.add(MAIL_REQUEST_TYPE, mailRequestType);
		}
		final String replyTo = messageHeaders.get("nats_replyTo", String.class);
		if (replyTo != null) {
			headers.add("nats_replyTo", replyTo);
		}
		return headers;
	}

	/**
	 * Method to generate a map of headers using the information from Nats message headers. This
	 * header map is then used to populate the headers of spring integration message
	 *
	 * @param msg NATS message from which the header information is extracted
	 * @return map of Headers
	 */
	@NonNull
	public static Map<String, Object> enrichMessageHeader(@NonNull final io.nats.client.Message msg) {
		final Map<String, Object> headersToCopy = new HashMap<>();
		if (msg.hasHeaders()) {
			final String ipgContextMessageKeyVal = msg.getHeaders().getFirst(IPG_CONTEXT_MESSAGE_KEY);
			if (ipgContextMessageKeyVal != null) {
				headersToCopy.put(IPG_CONTEXT_MESSAGE_KEY, ipgContextMessageKeyVal);
			}
			final String mailRequestType = msg.getHeaders().getFirst(MAIL_REQUEST_TYPE);
			if (mailRequestType != null) {
				headersToCopy.put(MAIL_REQUEST_TYPE, mailRequestType);
			}
			headersToCopy.put("nats_headers", msg.getHeaders());
		}
		if (msg.isJetStream()) {
			headersToCopy.put("nats_metadata", msg.metaData());
			headersToCopy.put("nats_isJetStream", msg.isJetStream());
		}
		headersToCopy.put("nats_subject", msg.getSubject());
		headersToCopy.put("nats_replyTo", msg.getReplyTo());
		return headersToCopy;
	}
}

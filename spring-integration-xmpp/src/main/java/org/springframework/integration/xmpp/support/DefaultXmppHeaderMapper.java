/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.xmpp.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.jivesoftware.smackx.jiveproperties.packet.JivePropertiesExtension;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import org.springframework.integration.mapping.AbstractHeaderMapper;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link XmppHeaderMapper}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Florian Schmaus
 * @author Stephane Nicoll
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class DefaultXmppHeaderMapper extends AbstractHeaderMapper<Message> implements XmppHeaderMapper {

	private static final List<String> STANDARD_HEADER_NAMES = new ArrayList<>();

	static {
		STANDARD_HEADER_NAMES.add(XmppHeaders.FROM);
		STANDARD_HEADER_NAMES.add(XmppHeaders.SUBJECT);
		STANDARD_HEADER_NAMES.add(XmppHeaders.THREAD);
		STANDARD_HEADER_NAMES.add(XmppHeaders.TO);
		STANDARD_HEADER_NAMES.add(XmppHeaders.TYPE);
	}

	public DefaultXmppHeaderMapper() {
		super(XmppHeaders.PREFIX, STANDARD_HEADER_NAMES, STANDARD_HEADER_NAMES);
	}

	@Override
	protected Map<String, Object> extractStandardHeaders(Message source) {
		Map<String, Object> headers = new HashMap<>();
		Jid from = source.getFrom();
		if (from != null) {
			headers.put(XmppHeaders.FROM, from.toString());
		}
		String subject = source.getSubject();
		if (StringUtils.hasText(subject)) {
			headers.put(XmppHeaders.SUBJECT, subject);
		}
		String thread = source.getThread();
		if (StringUtils.hasText(thread)) {
			headers.put(XmppHeaders.THREAD, thread);
		}
		Jid to = source.getTo();
		if (to != null) {
			headers.put(XmppHeaders.TO, to.toString());
		}
		Message.Type type = source.getType();
		if (type != null) {
			headers.put(XmppHeaders.TYPE, type);
		}
		return headers;
	}

	@Override
	protected Map<String, Object> extractUserDefinedHeaders(Message source) {
		Map<String, Object> headers = new HashMap<>();
		JivePropertiesExtension jpe = (JivePropertiesExtension) source.getExtension(JivePropertiesExtension.NAMESPACE);
		if (jpe == null) {
			return headers;
		}
		for (String propertyName : jpe.getPropertyNames()) {
			headers.put(propertyName, JivePropertiesManager.getProperty(source, propertyName));
		}
		return headers;
	}

	@Override
	protected void populateStandardHeaders(Map<String, Object> headers, Message target) {
		String threadId = getHeaderIfAvailable(headers, XmppHeaders.THREAD, String.class);
		if (StringUtils.hasText(threadId)) {
			target.setThread(threadId);
		}
		populateToHeader(headers, target);

		populateFromHeader(headers, target);

		String subject = getHeaderIfAvailable(headers, XmppHeaders.SUBJECT, String.class);
		if (StringUtils.hasText(subject)) {
			target.setSubject(subject);
		}

		Object typeHeader = getHeaderIfAvailable(headers, XmppHeaders.TYPE, Object.class);
		if (typeHeader instanceof String) {
			try {
				typeHeader = Message.Type.valueOf((String) typeHeader);
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("XMPP Type must be either a valid [Message.Type] " +
							"enum value or a String representation of such.");
				}
			}
		}
		if (typeHeader instanceof Message.Type) {
			target.setType((Message.Type) typeHeader);
		}
	}

	private void populateToHeader(Map<String, Object> headers, Message target) {
		String to = getHeaderIfAvailable(headers, XmppHeaders.TO, String.class);
		if (StringUtils.hasText(to)) {
			try {
				target.setTo(JidCreate.from(to));
			}
			catch (XmppStringprepException e) {
				throw new IllegalStateException("Cannot parse 'xmpp_to' header value", e);
			}
		}
	}

	private void populateFromHeader(Map<String, Object> headers, Message target) {
		String from = getHeaderIfAvailable(headers, XmppHeaders.FROM, String.class);
		if (StringUtils.hasText(from)) {
			try {
				target.setFrom(JidCreate.from(from));
			}
			catch (XmppStringprepException e) {
				throw new IllegalStateException("Cannot parse 'xmpp_from' header value", e);
			}
		}
	}

	@Override
	protected void populateUserDefinedHeader(String headerName, Object headerValue, Message target) {
		JivePropertiesManager.addProperty(target, headerName, headerValue);
	}

}

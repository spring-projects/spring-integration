/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Florian Schmaus
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class DefaultXmppHeaderMapperTests {

	@Test
	public void fromHeadersStandardOutbound() {
		DefaultXmppHeaderMapper mapper = new DefaultXmppHeaderMapper();
		Map<String, Object> headerMap = new HashMap<>();
		headerMap.put("userDefined1", "foo");
		headerMap.put("userDefined2", "bar");
		headerMap.put(XmppHeaders.THREAD, "test.thread");
		headerMap.put(XmppHeaders.TO, "test.to");
		headerMap.put(XmppHeaders.FROM, "test.from");
		headerMap.put(XmppHeaders.SUBJECT, "test.subject");
		headerMap.put(XmppHeaders.TYPE, "headline");
		MessageHeaders headers = new MessageHeaders(headerMap);
		MessageBuilder target = StanzaBuilder.buildMessage();
		mapper.fromHeadersToRequest(headers, target);

		// "standard" XMPP headers
		assertThat(target.getThread()).isEqualTo("test.thread");
		assertThat(target.getTo().toString()).isEqualTo("test.to");
		assertThat(target.getFrom().toString()).isEqualTo("test.from");
		assertThat(target.getSubject()).isEqualTo("test.subject");
		assertThat(target.getType()).isEqualTo(Message.Type.headline);

		// user-defined headers not included by default
		assertThat(JivePropertiesManager.getProperty(target, "userDefined1")).isNull();
		assertThat(JivePropertiesManager.getProperty(target, "userDefined2")).isNull();

		// transient headers should not be copied
		assertThat(JivePropertiesManager.getProperty(target, "id")).isNull();
		assertThat(JivePropertiesManager.getProperty(target, "timestamp")).isNull();
	}

	@Test
	public void fromHeadersUserDefinedOnly() {
		DefaultXmppHeaderMapper mapper = new DefaultXmppHeaderMapper();
		mapper.setRequestHeaderNames("userDefined1", "userDefined2");
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put("userDefined1", "foo");
		headerMap.put("userDefined2", "bar");
		headerMap.put("userDefined3", "baz");
		headerMap.put(XmppHeaders.THREAD, "test.thread");
		headerMap.put(XmppHeaders.TO, "test.to");
		headerMap.put(XmppHeaders.FROM, "test.from");
		headerMap.put(XmppHeaders.SUBJECT, "test.subject");
		headerMap.put(XmppHeaders.TYPE, "headline");
		MessageHeaders headers = new MessageHeaders(headerMap);
		MessageBuilder target = StanzaBuilder.buildMessage();
		mapper.fromHeadersToRequest(headers, target);

		// "standard" XMPP headers not included
		assertThat(target.getThread()).isNull();
		Object to = target.getTo();
		assertThat(to).isNull();
		Object from = target.getFrom();
		assertThat(from).isNull();
		assertThat(target.getSubject()).isNull();
		assertThat(target.getType()).isNull();

		// user-defined headers are included if in the list
		assertThat(JivePropertiesManager.getProperty(target, "userDefined1")).isEqualTo("foo");
		assertThat(JivePropertiesManager.getProperty(target, "userDefined2")).isEqualTo("bar");

		// user-defined headers are not included if not in the list
		assertThat(JivePropertiesManager.getProperty(target, "userDefined3")).isNull();

		// transient headers should not be copied
		assertThat(JivePropertiesManager.getProperty(target, "id")).isNull();
		assertThat(JivePropertiesManager.getProperty(target, "timestamp")).isNull();
	}

	@Test
	public void toHeadersStandardOnly() throws XmppStringprepException {
		DefaultXmppHeaderMapper mapper = new DefaultXmppHeaderMapper();
		MessageBuilder source = StanzaBuilder.buildMessage()
				.ofType(Message.Type.headline)
				.to("test.to")
				.from("test.from")
				.setSubject("test.subject")
				.setThread("test.thread");
		JivePropertiesManager.addProperty(source, "userDefined1", "foo");
		JivePropertiesManager.addProperty(source, "userDefined2", "bar");
		Map<String, Object> headers = mapper.toHeadersFromRequest(source);
		assertThat(headers.get(XmppHeaders.TO).toString()).isEqualTo("test.to");
		assertThat(headers.get(XmppHeaders.FROM).toString()).isEqualTo("test.from");
		assertThat(headers.get(XmppHeaders.SUBJECT)).isEqualTo("test.subject");
		assertThat(headers.get(XmppHeaders.THREAD)).isEqualTo("test.thread");
		assertThat(headers.get(XmppHeaders.TYPE)).isEqualTo(Message.Type.headline);
		assertThat(headers.get("userDefined1")).isNull();
		assertThat(headers.get("userDefined2")).isNull();
	}

	@Test
	public void toHeadersUserDefinedOnly() throws XmppStringprepException {
		DefaultXmppHeaderMapper mapper = new DefaultXmppHeaderMapper();
		mapper.setReplyHeaderNames("userDefined*");
		MessageBuilder source = StanzaBuilder.buildMessage()
				.ofType(Message.Type.headline)
				.to("test.to")
				.from("test.from")
				.setSubject("test.subject")
				.setThread("test.thread");
		JivePropertiesManager.addProperty(source, "userDefined1", "foo");
		JivePropertiesManager.addProperty(source, "userDefined2", "bar");
		Map<String, Object> headers = mapper.toHeadersFromReply(source);
		assertThat(headers.get(XmppHeaders.TO)).isNull();
		assertThat(headers.get(XmppHeaders.FROM)).isNull();
		assertThat(headers.get(XmppHeaders.SUBJECT)).isNull();
		assertThat(headers.get(XmppHeaders.THREAD)).isNull();
		assertThat(headers.get(XmppHeaders.TYPE)).isNull();
		assertThat(headers.get("userDefined1")).isEqualTo("foo");
		assertThat(headers.get("userDefined2")).isEqualTo("bar");
	}

}

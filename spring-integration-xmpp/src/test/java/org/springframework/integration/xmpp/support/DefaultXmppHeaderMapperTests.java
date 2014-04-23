/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.xmpp.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.jiveproperties.JivePropertiesManager;
import org.junit.Test;
import org.springframework.messaging.MessageHeaders;
import org.springframework.integration.xmpp.XmppHeaders;

/**
 * @author Mark Fisher
 * @author Florian Schmaus
 * @since 2.1
 */
public class DefaultXmppHeaderMapperTests {

	@Test
	public void fromHeadersStandardOutbound() {
		DefaultXmppHeaderMapper mapper = new DefaultXmppHeaderMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put("userDefined1", "foo");
		headerMap.put("userDefined2", "bar");
		headerMap.put(XmppHeaders.THREAD, "test.thread");
		headerMap.put(XmppHeaders.TO, "test.to");
		headerMap.put(XmppHeaders.FROM, "test.from");
		headerMap.put(XmppHeaders.SUBJECT, "test.subject");
		headerMap.put(XmppHeaders.TYPE, "headline");
		MessageHeaders headers = new MessageHeaders(headerMap);
		Message target = new Message();
		mapper.fromHeadersToRequest(headers, target);

		// "standard" XMPP headers
		assertEquals("test.thread", target.getThread());
		assertEquals("test.to", target.getTo());
		assertEquals("test.from", target.getFrom());
		assertEquals("test.subject", target.getSubject());
		assertEquals(Message.Type.headline, target.getType());

		// user-defined headers not included by default
		assertNull(JivePropertiesManager.getProperty(target, "userDefined1"));
		assertNull(JivePropertiesManager.getProperty(target, "userDefined2"));

		// transient headers should not be copied
		assertNull(JivePropertiesManager.getProperty(target, "id"));
		assertNull(JivePropertiesManager.getProperty(target, "timestamp"));
	}

	@Test
	public void fromHeadersUserDefinedOnly() {
		DefaultXmppHeaderMapper mapper = new DefaultXmppHeaderMapper();
		mapper.setRequestHeaderNames(new String[] { "userDefined1", "userDefined2" });
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
		Message target = new Message();
		mapper.fromHeadersToRequest(headers, target);

		// "standard" XMPP headers not included
		assertNull(target.getThread());
		assertNull(target.getTo());
		assertNull(target.getFrom());
		assertNull(target.getSubject());
		assertEquals(Message.Type.normal, target.getType());

		// user-defined headers are included if in the list
		assertEquals("foo", JivePropertiesManager.getProperty(target, "userDefined1"));
		assertEquals("bar", JivePropertiesManager.getProperty(target, "userDefined2"));

		// user-defined headers are not included if not in the list
		assertNull(JivePropertiesManager.getProperty(target, "userDefined3"));

		// transient headers should not be copied
		assertNull(JivePropertiesManager.getProperty(target, "id"));
		assertNull(JivePropertiesManager.getProperty(target, "timestamp"));
	}

	@Test
	public void toHeadersStandardOnly() {
		DefaultXmppHeaderMapper mapper = new DefaultXmppHeaderMapper();
		Message source = new Message("test.to", Message.Type.headline);
		source.setFrom("test.from");
		source.setSubject("test.subject");
		source.setThread("test.thread");
		JivePropertiesManager.addProperty(source, "userDefined1", "foo");
		JivePropertiesManager.addProperty(source, "userDefined2", "bar");
		Map<String, Object> headers = mapper.toHeadersFromRequest(source);
		assertEquals("test.to", headers.get(XmppHeaders.TO));
		assertEquals("test.from", headers.get(XmppHeaders.FROM));
		assertEquals("test.subject", headers.get(XmppHeaders.SUBJECT));
		assertEquals("test.thread", headers.get(XmppHeaders.THREAD));
		assertEquals(Message.Type.headline, headers.get(XmppHeaders.TYPE));
		assertNull(headers.get("userDefined1"));
		assertNull(headers.get("userDefined2"));
	}

	@Test
	public void toHeadersUserDefinedOnly() {
		DefaultXmppHeaderMapper mapper = new DefaultXmppHeaderMapper();
		mapper.setReplyHeaderNames(new String[] { "userDefined*" });
		Message source = new Message("test.to", Message.Type.headline);
		source.setFrom("test.from");
		source.setSubject("test.subject");
		source.setThread("test.thread");
		JivePropertiesManager.addProperty(source, "userDefined1", "foo");
		JivePropertiesManager.addProperty(source, "userDefined2", "bar");
		Map<String, Object> headers = mapper.toHeadersFromReply(source);
		assertNull(headers.get(XmppHeaders.TO));
		assertNull(headers.get(XmppHeaders.FROM));
		assertNull(headers.get(XmppHeaders.SUBJECT));
		assertNull(headers.get(XmppHeaders.THREAD));
		assertNull(headers.get(XmppHeaders.TYPE));
		assertEquals("foo", headers.get("userDefined1"));
		assertEquals("bar", headers.get("userDefined2"));
	}

}

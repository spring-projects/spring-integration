/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.xmpp.presence;

import static junit.framework.Assert.assertEquals;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.mapping.MessageMappingException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xmpp.XmppHeaders;

/**
 * @author Oleg Zhurakousky
 *
 */
public class XmppPresenceMessageMapperTests {

	@Test
	public void testFromMessageWithPayloadPresence() throws Exception{
		Presence presence = new Presence(Type.available, "Hello", 1, Mode.chat);
		Message<?> message = MessageBuilder.withPayload(presence).build();
		XmppPresenceMessageMapper mapper = new XmppPresenceMessageMapper();
		Presence mappedPresence = mapper.fromMessage(message);
		assertEquals(Mode.chat, mappedPresence.getMode());
		assertEquals(Type.available, mappedPresence.getType());
		assertEquals("Hello", mappedPresence.getStatus());
		assertEquals(1, mappedPresence.getPriority());
	}
	
	@Test
	public void testFromMessageWithPayloadPresenceType() throws Exception{
		Message<?> message = MessageBuilder.withPayload(Type.available)
		.setHeader(XmppHeaders.PRESENCE_FROM, "oleg")
		.setHeader(XmppHeaders.PRESENCE_MODE, Mode.chat)
		.setHeader(XmppHeaders.PRESENCE_STATUS, "hello")
		.setHeader(XmppHeaders.PRESENCE_TYPE, Type.subscribed)
		.setHeader(XmppHeaders.PRESENCE_PRIORITY, 1)
		.build();
		XmppPresenceMessageMapper mapper = new XmppPresenceMessageMapper();
		Presence mappedPresence = mapper.fromMessage(message);
		assertEquals(Mode.chat, mappedPresence.getMode());
		assertEquals(Type.available, mappedPresence.getType());
		assertEquals("hello", mappedPresence.getStatus());
		assertEquals(1, mappedPresence.getPriority());
	}
	@Test
	public void testFromMessageWithPayloadPresenceTypeAndStringModeType() throws Exception{
		Message<?> message = MessageBuilder.withPayload(Type.available)
		.setHeader(XmppHeaders.PRESENCE_FROM, "oleg")
		.setHeader(XmppHeaders.PRESENCE_MODE, "chat")
		.setHeader(XmppHeaders.PRESENCE_STATUS, "hello")
		.setHeader(XmppHeaders.PRESENCE_TYPE, "subscribed")
		.setHeader(XmppHeaders.PRESENCE_PRIORITY, 1)
		.build();
		XmppPresenceMessageMapper mapper = new XmppPresenceMessageMapper();
		Presence mappedPresence = mapper.fromMessage(message);
		assertEquals(Mode.chat, mappedPresence.getMode());
		assertEquals(Type.available, mappedPresence.getType());
		assertEquals("hello", mappedPresence.getStatus());
		assertEquals(1, mappedPresence.getPriority());
	}
	
	@Test(expected=MessageMappingException.class)
	public void testFromMessageWithPayloadPresenceTypeUnsupportedMode() throws Exception{
		
		Message<?> message = MessageBuilder.withPayload(Type.available)
		.setHeader(XmppHeaders.PRESENCE_MODE, 1)
		.build();
		XmppPresenceMessageMapper mapper = new XmppPresenceMessageMapper();
		mapper.fromMessage(message);
	}
	
	@Test(expected=MessageMappingException.class)
	public void testFromMessageWithPayloadPresenceTypeUnsupportedType() throws Exception{
		
		Message<?> message = MessageBuilder.withPayload(Type.available)
		.setHeader(XmppHeaders.PRESENCE_TYPE, 1)
		.build();
		XmppPresenceMessageMapper mapper = new XmppPresenceMessageMapper();
		mapper.fromMessage(message);
	}
	
	@Test(expected=MessageMappingException.class)
	public void testFromMessageWithUnsupportedPayload() throws Exception{	
		Message<?> message = MessageBuilder.withPayload("hello").build();
		XmppPresenceMessageMapper mapper = new XmppPresenceMessageMapper();
		mapper.fromMessage(message);
	}
}

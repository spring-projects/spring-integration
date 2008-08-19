/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.mail;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.easymock.classextension.EasyMock;
import org.junit.Test;

import org.springframework.integration.message.MessageHeaders;
import org.springframework.util.ObjectUtils;

/**
 * @author Mark Fisher
 */
public class DefaultMailMessageHeaderMapperTests {

	@Test
	public void mapExactlyOneFromAttributeFromMimeMessage() throws MessagingException {
		DefaultMailMessageHeaderMapper mapper = new DefaultMailMessageHeaderMapper();
		MimeMessage mailMessageMock = EasyMock.createMock(MimeMessage.class);
		Address[] fromAddresses = new Address[] { new InternetAddress("from@example.org") };
		EasyMock.expect(mailMessageMock.getFrom()).andReturn(fromAddresses);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.BCC)).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.CC)).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.TO)).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getReplyTo()).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getSubject()).andReturn("mail test");
		EasyMock.replay(mailMessageMock);
		Map<String, Object> headers = mapper.mapToMessageHeaders(mailMessageMock);
		Object fromHeader = headers.get(MailHeaders.FROM);
		assertNotNull(fromHeader);
		assertTrue(fromHeader instanceof String);
		assertEquals("from@example.org", fromHeader);
	}

	@Test
	public void mapExactlyOneReplyToAttributeFromMimeMessage() throws MessagingException {
		DefaultMailMessageHeaderMapper mapper = new DefaultMailMessageHeaderMapper();
		MimeMessage mailMessageMock = EasyMock.createMock(MimeMessage.class);
		Address[] replyToAddresses = new Address[] { new InternetAddress("replyTo@example.org") };
		EasyMock.expect(mailMessageMock.getFrom()).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.BCC)).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.CC)).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.TO)).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getReplyTo()).andReturn(replyToAddresses);
		EasyMock.expect(mailMessageMock.getSubject()).andReturn("mail test");
		EasyMock.replay(mailMessageMock);
		Map<String, Object> headers = mapper.mapToMessageHeaders(mailMessageMock);
		Object replyToHeader = headers.get(MailHeaders.REPLY_TO);
		assertNotNull(replyToHeader);
		assertTrue(replyToHeader instanceof String);
		assertEquals("replyTo@example.org", replyToHeader);
	}

	@Test
	public void mapMultipleToAttributesFromMimeMessage() throws MessagingException {
		DefaultMailMessageHeaderMapper mapper = new DefaultMailMessageHeaderMapper();
		MimeMessage mailMessageMock = EasyMock.createMock(MimeMessage.class);
		Address[] toAddresses = new Address[] {
			new InternetAddress("a@example.org"), new InternetAddress("b@example.org"), new InternetAddress("c@example.org")
		};
		EasyMock.expect(mailMessageMock.getFrom()).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.BCC)).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.CC)).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getRecipients(RecipientType.TO)).andReturn(toAddresses);
		EasyMock.expect(mailMessageMock.getReplyTo()).andReturn(new Address[0]);
		EasyMock.expect(mailMessageMock.getSubject()).andReturn("mail test");
		EasyMock.replay(mailMessageMock);
		Map<String, Object> headers = mapper.mapToMessageHeaders(mailMessageMock);
		Object toHeader = headers.get(MailHeaders.TO);
		assertNotNull(toHeader);
		assertTrue(toHeader instanceof String[]);
		String[] addresses = (String[]) toHeader;
		assertTrue(ObjectUtils.containsElement(addresses, "a@example.org"));
		assertTrue(ObjectUtils.containsElement(addresses, "b@example.org"));
		assertTrue(ObjectUtils.containsElement(addresses, "c@example.org"));
	}

	@Test
	public void mapReplyToValueFromHeadersToMimeMessage() throws MessagingException {
		DefaultMailMessageHeaderMapper mapper = new DefaultMailMessageHeaderMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(MailHeaders.REPLY_TO, "replyTo@example.org");
		MessageHeaders headers = new MessageHeaders(headerMap);
		MimeMessage mailMessageMock = EasyMock.createNiceMock(MimeMessage.class);
		EasyMock.replay(mailMessageMock);
		MimeMessage mimeMessage = new MimeMessage(mailMessageMock);
		mapper.mapFromMessageHeaders(headers, mimeMessage);
		Address[] replyToAddresses = mimeMessage.getReplyTo();
		assertEquals(1, replyToAddresses.length);
		assertEquals("replyTo@example.org", replyToAddresses[0].toString());
	}

	@Test
	public void mapFromValueFromHeadersToMimeMessage() throws MessagingException {
		DefaultMailMessageHeaderMapper mapper = new DefaultMailMessageHeaderMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		headerMap.put(MailHeaders.FROM, "from@example.org");
		MessageHeaders headers = new MessageHeaders(headerMap);
		MimeMessage mailMessageMock = EasyMock.createNiceMock(MimeMessage.class);
		EasyMock.replay(mailMessageMock);
		MimeMessage mimeMessage = new MimeMessage(mailMessageMock);
		mapper.mapFromMessageHeaders(headers, mimeMessage);
		Address[] fromAddresses = mimeMessage.getFrom();
		assertEquals(1, fromAddresses.length);
		assertEquals("from@example.org", fromAddresses[0].toString());
	}

	@Test
	public void mapMultileToValuesFromHeadersToMimeMessage() throws MessagingException {
		DefaultMailMessageHeaderMapper mapper = new DefaultMailMessageHeaderMapper();
		Map<String, Object> headerMap = new HashMap<String, Object>();
		String[] addressStrings = new String[] { "a@example.org", "b@example.org", "c@example.org" };
		headerMap.put(MailHeaders.TO, addressStrings);
		MessageHeaders headers = new MessageHeaders(headerMap);
		MimeMessage mailMessageMock = EasyMock.createNiceMock(MimeMessage.class);
		EasyMock.replay(mailMessageMock);
		MimeMessage mimeMessage = new MimeMessage(mailMessageMock);
		mapper.mapFromMessageHeaders(headers, mimeMessage);
		Address[] toAddresses = mimeMessage.getRecipients(RecipientType.TO);
		assertEquals(3, toAddresses.length);
		assertTrue(ObjectUtils.containsElement(toAddresses, new InternetAddress("a@example.org")));
		assertTrue(ObjectUtils.containsElement(toAddresses, new InternetAddress("b@example.org")));
		assertTrue(ObjectUtils.containsElement(toAddresses, new InternetAddress("c@example.org")));
	}

}

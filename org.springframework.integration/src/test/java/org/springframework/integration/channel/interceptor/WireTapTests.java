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

package org.springframework.integration.channel.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Mark Fisher
 */
public class WireTapTests {

	@Test
	public void testWireTapWithNoSelectors() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		mainChannel.send(new StringMessage("testing"));
		Message<?> original = mainChannel.receive(0);
		assertNotNull(original);
		Message<?> duplicate = secondaryChannel.receive(0);
		assertNotNull(duplicate);
	}

	@Test
	public void testWireTapWithRejectingSelector() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		List<MessageSelector> selectors = new ArrayList<MessageSelector>();
		selectors.add(new TestSelector(true));
		selectors.add(new TestSelector(false));
		mainChannel.addInterceptor(new WireTap(secondaryChannel, selectors));
		mainChannel.send(new StringMessage("testing"));
		Message<?> original = mainChannel.receive(0);
		assertNotNull(original);
		Message<?> duplicate = secondaryChannel.receive(0);
		assertNull(duplicate);
	}

	@Test
	public void testWireTapWithAcceptingSelectors() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		List<MessageSelector> selectors = new ArrayList<MessageSelector>();
		selectors.add(new TestSelector(true));
		selectors.add(new TestSelector(true));
		mainChannel.addInterceptor(new WireTap(secondaryChannel, selectors));
		mainChannel.send(new StringMessage("testing"));
		Message<?> original = mainChannel.receive(0);
		assertNotNull(original);
		Message<?> duplicate = secondaryChannel.receive(0);
		assertNotNull(duplicate);
	}

	@Test
	public void testNewMessageIdGeneratedForDuplicate() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		mainChannel.send(new StringMessage("testing"));
		Message<?> original = mainChannel.receive(0);
		Message<?> duplicate = secondaryChannel.receive(0);	
		Object duplicateId = duplicate.getId();
		assertNotNull(duplicateId);
		assertFalse("message ids should not match", original.getId().equals(duplicateId));
	}

	@Test
	public void testOriginalIdStoredAsAttribute() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		mainChannel.send(new StringMessage("testing"));
		Message<?> original = mainChannel.receive(0);
		Message<?> duplicate = secondaryChannel.receive(0);
		Object originalIdAttribute = duplicate.getHeader().getAttribute(WireTap.ORIGINAL_MESSAGE_ID_KEY);
		assertNotNull(originalIdAttribute);
		assertEquals(original.getId(), originalIdAttribute);
	}

	@Test
	public void testNewTimestampGeneratedForDuplicate() throws InterruptedException {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		Message<?> message = new StringMessage("testing");
		Thread.sleep(50);
		mainChannel.send(message);
		Message<?> original = mainChannel.receive(0);
		Message<?> duplicate = secondaryChannel.receive(0);
		assertTrue("original timestamp should precede duplicate",
				original.getHeader().getTimestamp() < duplicate.getHeader().getTimestamp());
	}

	public void testDuplicateMessageContainsAttribute() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		Message<?> message = new StringMessage("testing");
		String attributeKey = "testAttribute";
		Integer attributeValue = new Integer(123);
		message.getHeader().setAttribute(attributeKey, attributeValue);
		mainChannel.send(message);
		Message<?> original = mainChannel.receive(0);
		Message<?> duplicate = secondaryChannel.receive(0);
		Object originalAttribute = original.getHeader().getAttribute(attributeKey);
		Object duplicateAttribute = duplicate.getHeader().getAttribute(attributeKey);
		assertNotNull(originalAttribute);
		assertNotNull(duplicateAttribute);
		assertEquals(originalAttribute, duplicateAttribute);
	}

	@Test
	public void testDuplicateMessageContainsProperty() {
		QueueChannel mainChannel = new QueueChannel();
		QueueChannel secondaryChannel = new QueueChannel();
		mainChannel.addInterceptor(new WireTap(secondaryChannel));
		Message<?> message = new StringMessage("testing");
		String propertyKey = "testProperty";
		String propertyValue = "foo";
		message.getHeader().setProperty(propertyKey, propertyValue);
		mainChannel.send(message);
		Message<?> original = mainChannel.receive(0);
		Message<?> duplicate = secondaryChannel.receive(0);
		String originalProperty = original.getHeader().getProperty(propertyKey);
		String duplicateProperty = duplicate.getHeader().getProperty(propertyKey);
		assertNotNull(originalProperty);
		assertNotNull(duplicateProperty);
		assertEquals(originalProperty, duplicateProperty);
	}


	private static class TestSelector implements MessageSelector {

		private boolean shouldAccept;

		public TestSelector(boolean shouldAccept) {
			this.shouldAccept = shouldAccept;
		}

		public boolean accept(Message<?> message) {
			return this.shouldAccept;
		}
	}

}

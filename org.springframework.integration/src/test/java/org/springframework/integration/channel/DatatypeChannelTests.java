/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.channel;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;

import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class DatatypeChannelTests {

	@Test
	public void supportedType() {
		MessageChannel channel = createChannel(String.class);
		assertTrue(channel.send(new StringMessage("test")));
	}

	@Test(expected = MessageDeliveryException.class)
	public void unsupportedTypeAndNoConversionService() {
		MessageChannel channel = createChannel(Integer.class);
		channel.send(new StringMessage("test"));
	}

	@Test
	public void multipleTypes() {
		MessageChannel channel = createChannel(String.class, Integer.class);
		assertTrue(channel.send(new StringMessage("test1")));
		assertTrue(channel.send(new GenericMessage<Integer>(2)));
		Exception exception = null;
		try {
			channel.send(new GenericMessage<Date>(new Date()));
		}
		catch (MessageDeliveryException e) {
			exception = e;
		}
		assertNotNull(exception);
	}

	@Test
	public void subclassOfAcceptedType() {
		MessageChannel channel = createChannel(RuntimeException.class);
		assertTrue(channel.send(new ErrorMessage(new MessagingException("test"))));
	}

	@Test(expected = MessageDeliveryException.class)
	public void superclassOfAcceptedTypeNotAccepted() {
		MessageChannel channel = createChannel(RuntimeException.class);
		channel.send(new ErrorMessage(new Exception("test")));
	}


	private static QueueChannel createChannel(Class<?> ... datatypes) {
		QueueChannel channel = new QueueChannel();
		channel.setBeanName("testChannel");
		channel.setDatatypes(datatypes);
		return channel;
	}

}

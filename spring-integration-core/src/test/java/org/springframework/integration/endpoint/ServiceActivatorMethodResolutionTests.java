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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Mark Fisher
 */
public class ServiceActivatorMethodResolutionTests {

	@Test
	public void singleAnnotationMatches() {
		SingleAnnotationTestBean testBean = new SingleAnnotationTestBean();
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		QueueChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.handleMessage(new GenericMessage<String>("foo"));
		Message<?> result = outputChannel.receive(0);
		assertEquals("FOO", result.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void multipleAnnotationFails() {
		MultipleAnnotationTestBean testBean = new MultipleAnnotationTestBean();
		new ServiceActivatingHandler(testBean);
	}

	@Test
	public void singlePublicMethodMatches() {
		SinglePublicMethodTestBean testBean = new SinglePublicMethodTestBean();
		ServiceActivatingHandler serviceActivator = new ServiceActivatingHandler(testBean);
		QueueChannel outputChannel = new QueueChannel();
		serviceActivator.setOutputChannel(outputChannel);
		serviceActivator.handleMessage(new GenericMessage<String>("foo"));
		Message<?> result = outputChannel.receive(0);
		assertEquals("FOO", result.getPayload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void multiplePublicMethodFails() {
		MultiplePublicMethodTestBean testBean = new MultiplePublicMethodTestBean();
		new ServiceActivatingHandler(testBean);
	}


	@SuppressWarnings("unused")
	private static class SingleAnnotationTestBean {

		@ServiceActivator
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	@SuppressWarnings("unused")
	private static class MultipleAnnotationTestBean {

		@ServiceActivator
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		@ServiceActivator
		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	@SuppressWarnings("unused")
	private static class SinglePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		String lowerCase(String s) {
			return s.toLowerCase();
		}
	}


	@SuppressWarnings("unused")
	private static class MultiplePublicMethodTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}

}

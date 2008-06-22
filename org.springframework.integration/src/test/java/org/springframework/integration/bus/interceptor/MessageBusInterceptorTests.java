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

package org.springframework.integration.bus.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.integration.bus.MessageBus;

/**
 * @author Marius Bogoevici
 */
public class MessageBusInterceptorTests {

	@Test
	public void testStart() {
		MessageBus messageBus = new MessageBus();
		TestMessageBusStartInterceptor startInterceptor = new TestMessageBusStartInterceptor();
		TestMessageBusStopInterceptor stopInterceptor = new TestMessageBusStopInterceptor();
		// add all interceptors
		messageBus.addInterceptor(startInterceptor);
		messageBus.addInterceptor(stopInterceptor);
		// check the state of the interceptors
		executeInterceptorsTest(messageBus, startInterceptor, stopInterceptor);
	}

	public static void executeInterceptorsTest(MessageBus messageBus, TestMessageBusStartInterceptor startInterceptor,
	                                     TestMessageBusStopInterceptor stopInterceptor) {
		assertTrue(!messageBus.isRunning());
		assertEquals(startInterceptor.getPreStartCounter().get(), 0);
		assertEquals(startInterceptor.getPostStartCounter().get(), 0);
		assertEquals(stopInterceptor.getPreStopCounter().get(), 0);
		assertEquals(stopInterceptor.getPostStopCounter().get(), 0);
		// start the bus
		messageBus.start();
		// check the state of the interceptors
		assertEquals(startInterceptor.getPreStartCounter().get(), 1);
		assertEquals(startInterceptor.getPostStartCounter().get(), 1);
		assertEquals(stopInterceptor.getPreStopCounter().get(), 0);
		assertEquals(stopInterceptor.getPostStopCounter().get(), 0);
		//stop the bus
		messageBus.stop();
		//check the state of the interceptors
		assertEquals(startInterceptor.getPreStartCounter().get(), 1);
		assertEquals(startInterceptor.getPostStartCounter().get(), 1);
		assertEquals(stopInterceptor.getPreStopCounter().get(), 1);
		assertEquals(stopInterceptor.getPostStopCounter().get(), 1);
	}


}

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

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.integration.bus.MessageBus;

/**
 * @author Marius Bogoevici
*/
public class TestMessageBusStopInterceptor extends MessageBusInterceptorAdapter {

	private AtomicInteger preStopCounter = new AtomicInteger(0);

	private AtomicInteger postStopCounter = new AtomicInteger(0);


	public AtomicInteger getPreStopCounter() {
		return preStopCounter;
	}

	public AtomicInteger getPostStopCounter() {
		return postStopCounter;
	}

	@Override
	public void preStop(MessageBus bus) {
		this.preStopCounter.incrementAndGet();
		org.junit.Assert.assertTrue(bus.isRunning());
	}

	@Override
	public void postStop(MessageBus bus) {
		this.postStopCounter.incrementAndGet();
		org.junit.Assert.assertTrue(!bus.isRunning());
	}

}

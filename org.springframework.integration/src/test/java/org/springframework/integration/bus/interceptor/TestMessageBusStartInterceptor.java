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
public class TestMessageBusStartInterceptor extends MessageBusInterceptorAdapter {

	private AtomicInteger preStartCounter = new AtomicInteger(0);

	private AtomicInteger postStartCounter = new AtomicInteger(0);


	public AtomicInteger getPreStartCounter() {
		return preStartCounter;
	}

	public AtomicInteger getPostStartCounter() {
		return postStartCounter;
	}

	@Override
	public void preStart(MessageBus bus) {
		this.preStartCounter.incrementAndGet();
		org.junit.Assert.assertTrue(!bus.isRunning());
	}

	@Override
	public void postStart(MessageBus bus) {
		this.postStartCounter.incrementAndGet();
		org.junit.Assert.assertTrue(bus.isRunning());
	}

}

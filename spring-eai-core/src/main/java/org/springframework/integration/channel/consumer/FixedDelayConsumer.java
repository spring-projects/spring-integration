/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.channel.consumer;

import java.util.concurrent.TimeUnit;

import org.springframework.integration.MessageSource;
import org.springframework.integration.handler.MessageHandler;

/**
 * A consumer that measures the <code>pollInterval</code> between the
 * invoker's execution completion time and the subsequent start.
 * 
 * @author Mark Fisher
 */
public class FixedDelayConsumer extends AbstractPollingConsumer {

	public FixedDelayConsumer(MessageSource source, MessageHandler handler) {
		super(source, handler);
	}


	@Override
	protected void scheduleInvoker(Runnable invoker, int initialDelay, int pollInterval, TimeUnit timeUnit) {
		this.executor.scheduleWithFixedDelay(invoker, initialDelay, pollInterval, timeUnit);
	}

}

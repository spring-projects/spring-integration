/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.monitor;

import java.util.concurrent.atomic.AtomicLong;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.integration.core.MessageSource;

/**
 * @author Dave Syer
 * @since 2.0
 */
public class SimpleMessageSourceMetrics implements MethodInterceptor, MessageSourceMetrics {

	private final AtomicLong messageCount = new AtomicLong();

	private final MessageSource<?> messageSource;

	private volatile String source;

	private volatile String name;


	public SimpleMessageSourceMetrics(MessageSource<?> messageSource) {
		this.messageSource = messageSource;
	}


	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return this.source;
	}

	public MessageSource<?> getMessageSource() {
		return this.messageSource;
	}

	public void reset() {
		this.messageCount.set(0);
	}

	public int getMessageCount() {
		return (int) this.messageCount.get();
	}

	public long getMessageCountLong() {
		return this.messageCount.get();
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		String method = invocation.getMethod().getName();
		Object result = invocation.proceed();
		if ("receive".equals(method) && result!=null) {
			this.messageCount.incrementAndGet();
		}
		return result;
	}

	@Override
	public String toString() {
		return String.format("MessageSourceMonitor: [name=%s, source=%s, count=%d]", name, source, messageCount.get());
	}

}

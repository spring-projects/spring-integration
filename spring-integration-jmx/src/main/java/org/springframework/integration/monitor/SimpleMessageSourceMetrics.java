/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.integration.core.MessageSource;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.support.MetricType;

/**
 * @author Dave Syer
 *
 * @since 2.0
 */
public class SimpleMessageSourceMetrics implements MethodInterceptor, MessageSourceMetrics {

	private final AtomicInteger messageCount = new AtomicInteger();

	private final MessageSource<?> messageSource;

	private String source;

	private String name;

	public SimpleMessageSourceMetrics(MessageSource<?> messageSource) {
		this.messageSource = messageSource;	
	}
	

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSource() {
		return this.source;
	}

	public MessageSource<?> getMessageSource() {
		return messageSource;
	}

	@ManagedOperation
	public void reset() {
		messageCount.set(0);
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Message Source Message Count")
	public int getMessageCount() {
		return messageCount.get();
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		String method = invocation.getMethod().getName();
		Object result = invocation.proceed();
		if ("receive".equals(method) && result!=null) {
			messageCount.incrementAndGet();
		}
		return result;
	}

	@Override
	public String toString() {
		return String.format("MessageSourceMonitor: [name=%s, source=%s, count=%d]", name, source, messageCount.get());
	}

}

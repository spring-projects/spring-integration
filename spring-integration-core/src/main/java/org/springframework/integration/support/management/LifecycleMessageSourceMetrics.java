/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.support.management;

import org.springframework.context.Lifecycle;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * A {@link MessageSourceMetrics} that exposes in addition the {@link Lifecycle} interface.
 * The lifecycle methods can be used to start and stop polling endpoints, for instance, in a live system.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@IntegrationManagedResource
public class LifecycleMessageSourceMetrics implements MessageSourceMetrics, Lifecycle {

	private final Lifecycle lifecycle;

	protected final MessageSourceMetrics delegate;


	public LifecycleMessageSourceMetrics(Lifecycle lifecycle, MessageSourceMetrics delegate) {
		this.lifecycle = lifecycle;
		this.delegate = delegate;
	}

	public MessageSourceMetrics getDelegate() {
		return this.delegate;
	}

	@Override
	@ManagedOperation
	public void reset() {
		this.delegate.reset();
	}

	@Override
	@ManagedAttribute
	public boolean isRunning() {
		return this.lifecycle.isRunning();
	}

	@Override
	@ManagedOperation
	public void start() {
		this.lifecycle.start();
	}

	@Override
	@ManagedOperation
	public void stop() {
		this.lifecycle.stop();
	}

	@Override
	public String getManagedName() {
		return this.delegate.getManagedName();
	}

	@Override
	public String getManagedType() {
		return this.delegate.getManagedType();
	}

	@Override
	public int getMessageCount() {
		return this.delegate.getMessageCount();
	}

	@Override
	public long getMessageCountLong() {
		return this.delegate.getMessageCountLong();
	}

	@Override
	public void setCountsEnabled(boolean countsEnabled) {
		this.delegate.setCountsEnabled(countsEnabled);
	}

	@Override
	public boolean isCountsEnabled() {
		return this.delegate.isCountsEnabled();
	}

	@Override
	public void setLoggingEnabled(boolean enabled) {
		this.delegate.setLoggingEnabled(enabled);
	}

	@Override
	public boolean isLoggingEnabled() {
		return this.delegate.isLoggingEnabled();
	}

	@Override
	public void setManagedName(String name) {
		this.delegate.setManagedName(name);
	}

	@Override
	public void setManagedType(String source) {
		this.delegate.setManagedType(source);
	}

	@Override
	public ManagementOverrides getOverrides() {
		return this.delegate.getOverrides();
	}

}

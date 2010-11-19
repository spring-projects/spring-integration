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

package org.springframework.integration.monitor;

import org.springframework.context.Lifecycle;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * A {@link MessageHandlerMetrics} that exposes in addition the {@link Lifecycle} interface. The lifecycle methods can
 * be used to stop and start polling endpoints, for instance, in a live system.
 * 
 * @author Dave Syer
 * @since 2.0
 */
@ManagedResource
public class LifecycleMessageHandlerMetrics implements MessageHandlerMetrics, Lifecycle {

	private final Lifecycle lifecycle;

	private final MessageHandlerMetrics delegate;


	public LifecycleMessageHandlerMetrics(Lifecycle lifecycle, MessageHandlerMetrics delegate) {
		this.lifecycle = lifecycle;
		this.delegate = delegate;
	}


	@ManagedAttribute
	public boolean isRunning() {
		return this.lifecycle.isRunning();
	}

	@ManagedOperation
	public void start() {
		this.lifecycle.start();
	}

	@ManagedOperation
	public void stop() {
		this.lifecycle.stop();
	}

	public void reset() {
		this.delegate.reset();
	}

	public int getErrorCount() {
		return this.delegate.getErrorCount();
	}

	public int getHandleCount() {
		return this.delegate.getHandleCount();
	}

	public double getMaxDuration() {
		return this.delegate.getMaxDuration();
	}

	public double getMeanDuration() {
		return this.delegate.getMeanDuration();
	}

	public double getMinDuration() {
		return this.delegate.getMinDuration();
	}

	public double getStandardDeviationDuration() {
		return this.delegate.getStandardDeviationDuration();
	}

	public Statistics getDuration() {
		return this.delegate.getDuration();
	}

	public String getName() {
		return this.delegate.getName();
	}

	public String getSource() {
		return this.delegate.getSource();
	}

	public int getActiveCount() {
		return this.delegate.getActiveCount();
	}

}

/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.monitor;

import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Wrapper for an {@link AbstractEndpoint} that exposes a management interface.
 *
 * @author Dave Syer
 * @author Gary Russell
 *
 * @deprecated this is no longer used by the framework. Replaced by
 * {@link org.springframework.integration.support.management.ManageableLifecycle}.
 *
 */
@Deprecated
@IntegrationManagedResource
public class ManagedEndpoint implements Lifecycle {

	private final AbstractEndpoint delegate;

	public ManagedEndpoint(AbstractEndpoint delegate) {
		this.delegate = delegate;
	}

	@Override
	@ManagedAttribute
	public final boolean isRunning() {
		return this.delegate.isRunning();
	}

	@Override
	@ManagedOperation
	public final void start() {
		this.delegate.start();
	}

	@Override
	@ManagedOperation
	public final void stop() {
		this.delegate.stop();
	}

}

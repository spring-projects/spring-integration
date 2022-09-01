/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.integration.support.management;

import org.springframework.context.Lifecycle;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Makes {@link Lifecycle} methods manageable.
 *
 * @author Gary Russell
 * @since 5.4
 *
 */
public interface ManageableLifecycle extends Lifecycle {

	@ManagedOperation(description = "Start the component")
	@Override
	void start();

	@ManagedOperation(description = "Stop the component")
	@Override
	void stop();

	@ManagedAttribute(description = "Is the component running?")
	@Override
	boolean isRunning();

}

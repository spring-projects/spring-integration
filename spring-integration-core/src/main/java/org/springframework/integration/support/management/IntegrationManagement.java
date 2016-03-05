/*
 * Copyright 2015-2016 the original author or authors.
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

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Base interface for Integration managed components.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public interface IntegrationManagement {

	@ManagedAttribute(description = "Use to disable debug logging during normal message flow")
	void setLoggingEnabled(boolean enabled);

	@ManagedAttribute
	boolean isLoggingEnabled();

	@ManagedOperation
	void reset();

	@ManagedAttribute(description = "Enable message counting statistics")
	void setCountsEnabled(boolean countsEnabled);

	@ManagedAttribute
	boolean isCountsEnabled();

}

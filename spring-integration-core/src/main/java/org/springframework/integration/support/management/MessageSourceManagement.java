/*
 * Copyright 2016-2020 the original author or authors.
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

import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * Message sources implementing this interface have additional properties that
 * can be set or examined using JMX.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
@IntegrationManagedResource
public interface MessageSourceManagement {

	/**
	 * Set the maximum number of objects the source should fetch if it is necessary to
	 * fetch objects. Setting the
	 * maxFetchSize to 0 disables remote fetching, a negative value indicates no limit.
	 * @param maxFetchSize the max fetch size; a negative value means unlimited.
	 */
	@ManagedAttribute(description = "Maximum objects to fetch")
	void setMaxFetchSize(int maxFetchSize);

	/**
	 * Return the max fetch size.
	 * @return the max fetch size.
	 * @see #setMaxFetchSize(int)
	 */
	@ManagedAttribute
	int getMaxFetchSize();

}

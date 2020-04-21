/*
 * Copyright 2015-2020 the original author or authors.
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
 * Base interface containing methods to control complete statistics gathering.
 *
 * @author Gary Russell
 * @since 4.2
 *
 * @deprecated in favor of Micrometer metrics.
 */
@Deprecated
public interface IntegrationStatsManagement extends IntegrationManagement {

	@ManagedAttribute(description = "Enable all statistics")
	void setStatsEnabled(boolean statsEnabled);

	@ManagedAttribute
	boolean isStatsEnabled();

}

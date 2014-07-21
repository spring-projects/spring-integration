/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.router;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Exposes adding/removing individual recipients operations for 
 * RecipientListRouter. This can be used with a control-bus and JMX.
 * 
 * @author Liujiong
 * @since 4.1
 *
 */
@ManagedResource
public interface RecipientListRouterManagement {

	@ManagedOperation
	void addRecipient(String channelName, String expression);
	
	@ManagedOperation
	void addRecipient(String channelName);
	
	@ManagedOperation
	void removeRecipient(String channelName);

	@ManagedOperation
	void removeRecipient(String channelName, String selector);
	
}

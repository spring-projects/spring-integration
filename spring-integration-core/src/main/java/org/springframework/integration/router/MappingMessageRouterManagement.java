/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public interface MappingMessageRouterManagement {

	/**
	 * Add a channel mapping from the provided key to channel name.
	 */
	@ManagedOperation
	public abstract void setChannelMapping(String key, String channelName);

	/**
	 * Remove a channel mapping for the given key if present.
	 */
	@ManagedOperation
	public abstract void removeChannelMapping(String key);

}
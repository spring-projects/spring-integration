/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * A registry for maintaining message endpoints in a map keyed by name.
 * 
 * @author Mark Fisher
 */
public class EndpointRegistry {

	private Map<String, MessageEndpoint> mappings = new ConcurrentHashMap<String, MessageEndpoint>();


	public void register(String name, MessageEndpoint endpoint) {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(endpoint, "endpoint must not be null");
		this.mappings.put(name, endpoint);
	}

	public MessageEndpoint lookup(String name) {
		return this.mappings.get(name);
	}

}

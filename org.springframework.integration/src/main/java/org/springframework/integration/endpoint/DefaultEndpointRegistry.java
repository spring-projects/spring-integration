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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.util.Assert;

/**
 * A simple map-backed implementation of {@link EndpointRegistry}.
 * 
 * @author Mark Fisher
 */
public class DefaultEndpointRegistry implements EndpointRegistry {

	private final Map<String, MessageEndpoint> endpoints = new ConcurrentHashMap<String, MessageEndpoint>();


	public MessageEndpoint lookupEndpoint(String endpointName) {
		return this.endpoints.get(endpointName);
	}

	public void registerEndpoint(MessageEndpoint endpoint) {
		Assert.notNull(endpoint, "'endpoint' must not be null");
		this.endpoints.put(endpoint.getName(), endpoint);
	}

	public MessageEndpoint unregisterEndpoint(String name) {
		return (name != null) ? this.endpoints.remove(name) : null;
	}

	public Set<String> getEndpointNames() {
		return this.endpoints.keySet();
	}

}

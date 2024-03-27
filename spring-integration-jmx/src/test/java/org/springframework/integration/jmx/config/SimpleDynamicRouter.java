/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jmx.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 *
 */

/**
 *
 *
 */
@ManagedResource
public class SimpleDynamicRouter {

	private final Map<String, String> channelMappings = new HashMap<String, String>();

	/**
	 *
	 * @param channelMappings
	 */
	public SimpleDynamicRouter(Map<String, String> channelMappings) {
		Assert.notEmpty(channelMappings, "you must provide at least one channel mappings");
		for (String key : channelMappings.keySet()) {
			this.channelMappings.put(key, channelMappings.get(key));
		}
	}

	/**
	 *
	 * @param key
	 * @param channelName
	 */
	@ManagedOperation
	public void addChannelMapping(String key, String channelName) {
		this.channelMappings.put(key, channelName);
	}

	/**
	 *
	 * @param key
	 */
	public void removeChannelMapping(String key) {
		this.channelMappings.remove(key);
	}

	/**
	 *
	 */
	public Map<String, String> getChannelMappings() {
		return channelMappings;
	}

	/**
	 *
	 * @param key
	 */
	public String route(Object key) {
		String className = key.getClass().getName();
		return this.channelMappings.get(className);
	}

}

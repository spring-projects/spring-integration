/*
 * Copyright 2002-present the original author or authors.
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
@ManagedResource
public class SimpleDynamicRouter {

	private final Map<String, String> channelMappings = new HashMap<String, String>();

	public SimpleDynamicRouter(Map<String, String> channelMappings) {
		Assert.notEmpty(channelMappings, "you must provide at least one channel mappings");
		for (String key : channelMappings.keySet()) {
			this.channelMappings.put(key, channelMappings.get(key));
		}
	}

	@ManagedOperation
	public void addChannelMapping(String key, String channelName) {
		this.channelMappings.put(key, channelName);
	}

	public void removeChannelMapping(String key) {
		this.channelMappings.remove(key);
	}

	public Map<String, String> getChannelMappings() {
		return channelMappings;
	}

	public String route(Object key) {
		String className = key.getClass().getName();
		return this.channelMappings.get(className);
	}

}

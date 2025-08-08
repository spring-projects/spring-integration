/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
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

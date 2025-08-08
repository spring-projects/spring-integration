/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.metadata;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Strategy interface for storing metadata from certain adapters
 * to avoid duplicate delivery of messages, for example.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@ManagedResource
public interface MetadataStore {

	/**
	 * Writes a key value pair to this MetadataStore.
	 *
	 * @param key The key.
	 * @param value The value.
	 */
	void put(String key, String value);

	/**
	 * Reads a value for the given key from this MetadataStore.
	 *
	 * @param key The key.
	 * @return The value.
	 */
	@ManagedAttribute
	String get(String key);

	/**
	 * Remove a value for the given key from this MetadataStore.
	 * @param key The key.
	 * @return The previous value associated with key, or
	 *         null if there was no mapping for key.
	 */
	@ManagedAttribute
	String remove(String key);

}

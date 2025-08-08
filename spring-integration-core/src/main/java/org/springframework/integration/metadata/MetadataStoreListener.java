/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.metadata;

/**
 * A callback to be invoked whenever a value changes in the data store.
 *
 * @author Marius Bogoevici
 *
 * @since 4.2
 */
public interface MetadataStoreListener {

	/**
	 * Invoked when a key is added to the store.
	 * @param key the key being added
	 * @param value the value being added
	 */
	void onAdd(String key, String value);

	/**
	 * Invoked when a key is removed from the store.
	 * @param key the key being removed
	 * @param oldValue the value being removed
	 */
	void onRemove(String key, String oldValue);

	/**
	 * Invoked when a key is updated into the store.
	 * @param key the key being updated
	 * @param newValue the new value
	 */
	void onUpdate(String key, String newValue);

}

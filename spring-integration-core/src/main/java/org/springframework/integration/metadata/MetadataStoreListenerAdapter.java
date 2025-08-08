/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.metadata;

/**
 * Base implementation for a {@link MetadataStoreListener}. Subclasses may override any of the methods.
 *
 * @author Marius Bogoevici
 */
public abstract class MetadataStoreListenerAdapter implements MetadataStoreListener {

	@Override
	public void onAdd(String key, String value) {

	}

	@Override
	public void onRemove(String key, String oldValue) {

	}

	@Override
	public void onUpdate(String key, String newValue) {

	}

}

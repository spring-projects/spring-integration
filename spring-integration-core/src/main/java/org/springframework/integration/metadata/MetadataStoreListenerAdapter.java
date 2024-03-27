/*
 * Copyright 2015-2024 the original author or authors.
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

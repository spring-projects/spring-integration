/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.selector;

import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.MetadataKeyStrategy;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link MessageSelector} implementation around the {@link ConcurrentMetadataStore}
 * and {@link MetadataKeyStrategy}.
 * <p>
 * The {@link #accept} method extracts {@code metadataKey} from the provided {@code message}
 * using {@link MetadataKeyStrategy} and use {@code id} header as a {@code value}.
 * <p>
 * The successful result of the {@link #accept} method is based on the
 * {@link ConcurrentMetadataStore#putIfAbsent} return value. The {@code true} is returned
 * from this {@link MessageSelector} if {@code putIfAbsent} has returned {@code null}.
 * And at the same time it means that the value is placed to the {@code MetadataStore}.
 * Otherwise the messages isn't accepted because there is already a value in the
 * {@code MetadataStore} associated with the {@code key}.
 * <p>
 * This {@link MessageSelector} is useful for the custom
 * <a href="http://www.eaipatterns.com/IdempotentReceiver.html">Idempotent Receiver</a>
 * implementation.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class MetadataStoreSelector implements MessageSelector {

	private final ConcurrentMetadataStore metadataStore;

	private final MetadataKeyStrategy keyStrategy;

	public MetadataStoreSelector(MetadataKeyStrategy keyStrategy) {
		this(keyStrategy, new SimpleMetadataStore());
	}

	public MetadataStoreSelector(MetadataKeyStrategy keyStrategy, ConcurrentMetadataStore metadataStore) {
		Assert.notNull(metadataStore);
		Assert.notNull(keyStrategy);
		this.metadataStore = metadataStore;
		this.keyStrategy = keyStrategy;
	}

	@Override
	public boolean accept(Message<?> message) {
		String key = this.keyStrategy.getKey(message);
		String value = message.getHeaders().getId().toString();

		return this.metadataStore.putIfAbsent(key, value) == null;
	}

}

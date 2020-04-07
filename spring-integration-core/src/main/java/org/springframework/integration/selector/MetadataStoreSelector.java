/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.selector;

import java.util.function.BiPredicate;

import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * The {@link MessageSelector} implementation using a {@link ConcurrentMetadataStore}
 * and {@link MessageProcessor}.
 * <p>
 * The {@link #accept} method extracts {@code metadataKey} from the provided {@code message}
 * using {@link MessageProcessor} and uses the {@code timestamp} header as the {@code value}
 * (hex) by default. The {@link #valueStrategy} can be provided to override the default behaviour.
 * <p>
 * The successful result of the {@link #accept} method is based on the
 * {@link ConcurrentMetadataStore#putIfAbsent} return value. {@code true} is returned
 * if {@code putIfAbsent} returns {@code null}.
 * And, at the same time, it means that the value has been placed in the {@code MetadataStore}.
 * Otherwise the messages isn't accepted because there is already a value in the
 * {@code MetadataStore} associated with the {@code key}.
 * <p>
 * This {@link MessageSelector} is useful for an
 * <a href="https://www.enterpriseintegrationpatterns.com/IdempotentReceiver.html">Idempotent Receiver</a>
 * implementation.
 * <p>
 * It can be used in a {@link org.springframework.integration.filter.MessageFilter}
 * or {@link org.springframework.integration.handler.advice.IdempotentReceiverInterceptor}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
public class MetadataStoreSelector implements MessageSelector {

	private final ConcurrentMetadataStore metadataStore;

	private final MessageProcessor<String> keyStrategy;

	private final MessageProcessor<String> valueStrategy;

	@Nullable
	private BiPredicate<String, String> compareValues;

	public MetadataStoreSelector(MessageProcessor<String> keyStrategy) {
		this(keyStrategy, (MessageProcessor<String>) null);
	}

	public MetadataStoreSelector(MessageProcessor<String> keyStrategy, MessageProcessor<String> valueStrategy) {
		this(keyStrategy, valueStrategy, new SimpleMetadataStore());
	}

	public MetadataStoreSelector(MessageProcessor<String> keyStrategy, ConcurrentMetadataStore metadataStore) {
		this(keyStrategy, null, metadataStore);
	}

	public MetadataStoreSelector(MessageProcessor<String> keyStrategy, MessageProcessor<String> valueStrategy,
			ConcurrentMetadataStore metadataStore) {
		Assert.notNull(keyStrategy, "'keyStrategy' must not be null");
		Assert.notNull(metadataStore, "'metadataStore' must not be null");
		this.metadataStore = metadataStore;
		this.keyStrategy = keyStrategy;
		this.valueStrategy = valueStrategy;
	}

	/**
	 * Set a {@link BiPredicate} to compare old and new values in the metadata store for
	 * the key. The first parameter is the old value; return true if we should accept this
	 * message and replace the old value with the new value.
	 * @param compareValues the {@link BiPredicate}.
	 * @since 5.3
	 */
	public void setCompareValues(@Nullable BiPredicate<String, String> compareValues) {
		this.compareValues = compareValues;
	}

	/**
	 * Fluent version of {@link #setCompareValues(BiPredicate)}.
	 * @param compareValues the {@link BiPredicate}.
	 * @return this.
	 */
	public MetadataStoreSelector compareValues(@Nullable BiPredicate<String, String> compareValues) {
		setCompareValues(compareValues);
		return this;
	}

	@Override
	public boolean accept(Message<?> message) {
		String key = this.keyStrategy.processMessage(message);
		Long timestamp = message.getHeaders().getTimestamp();
		String value = (this.valueStrategy != null)
				? this.valueStrategy.processMessage(message)
				: (timestamp == null ? "0" : Long.toString(timestamp));

		BiPredicate<String, String> predicate = this.compareValues;
		if (predicate == null) {
			return this.metadataStore.putIfAbsent(key, value) == null;
		}
		else {
			synchronized (this) {
				String oldValue = this.metadataStore.get(key);
				if (oldValue == null) {
					return this.metadataStore.putIfAbsent(key, value) == null;
				}
				if (predicate.test(oldValue, value)) {
					return this.metadataStore.replace(key, oldValue, value);
				}
				return false;
			}
		}
	}

}

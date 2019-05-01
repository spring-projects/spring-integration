/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.integration.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * A {@code Builder} pattern implementation for the {@link Map}.
 *
 * @param <B> The type of target {@link MapBuilder} implementation.
 * @param <K> The Map key type.
 * @param <V> The Map value type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class MapBuilder<B extends MapBuilder<B, K, V>, K, V> {

	protected static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final Map<K, V> map = new HashMap<>();

	public B put(K key, V value) {
		this.map.put(key, value);
		return _this();
	}

	public Map<K, V> get() {
		return this.map;
	}

	@SuppressWarnings("unchecked")
	protected final B _this() { // NOSONAR name
		return (B) this;
	}

}

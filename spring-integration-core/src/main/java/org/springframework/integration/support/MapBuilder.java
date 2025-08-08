/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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

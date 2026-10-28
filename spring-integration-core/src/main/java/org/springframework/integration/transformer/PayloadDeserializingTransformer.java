/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.core.serializer.Deserializer;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.util.Assert;

/**
 * Transformer that deserializes the inbound byte array payload to an object by delegating
 * to a Converter&lt;byte[], Object&gt;. Default delegate is a
 * {@link AllowListDeserializingConverter} using Java serialization.
 *
 * <p>
 * The byte array payload must be a result of equivalent serialization.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 1.0.1
 */
public class PayloadDeserializingTransformer extends PayloadTypeConvertingTransformer<byte[], Object> {

	/**
	 * Instantiate based on the {@link AllowListDeserializingConverter} with the
	 * {@link org.springframework.core.serializer.DefaultDeserializer}.
	 */
	@SuppressWarnings("this-escape")
	public PayloadDeserializingTransformer() {
		doSetConverter(new AllowListDeserializingConverter());
	}

	/**
	 * Set a {@link Deserializer} to be wrapped into a new {@link AllowListDeserializingConverter}.
	 * The allowed patterns of the current {@link AllowListDeserializingConverter}, if any,
	 * are carried over to the new one.
	 * @param deserializer the deserializer to use.
	 */
	public void setDeserializer(Deserializer<Object> deserializer) {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter(deserializer);
		if (getConverter() instanceof AllowListDeserializingConverter currentConverter) {
			converter.setAllowedPatterns(currentConverter.getAllowedPatterns().toArray(new String[0]));
		}
		setConverter(converter);
	}

	/**
	 * When using a {@link AllowListDeserializingConverter} (the default) set patterns
	 * for packages/classes that are allowed to be deserialized.
	 * A class can be fully qualified, or a wildcard '*' is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.example.*}, {@code *.MyClass}.
	 * Replaces all the existing patterns, including the
	 * {@link AllowListDeserializingConverter#DEFAULT_ALLOWED_PATTERNS}.
	 * Use {@link #addAllowedPatterns(String...)} to keep the defaults.
	 * @param patterns the patterns.
	 * @since 5.4
	 */
	public void setAllowedPatterns(String... patterns) {
		obtainAllowListDeserializingConverter().setAllowedPatterns(patterns);
	}

	/**
	 * When using a {@link AllowListDeserializingConverter} (the default) add patterns
	 * for packages/classes that are allowed to be deserialized, in addition to the existing ones
	 * (by default the {@link AllowListDeserializingConverter#DEFAULT_ALLOWED_PATTERNS}).
	 * @param patterns the patterns to add.
	 * @since 7.2
	 * @see #setAllowedPatterns(String...)
	 */
	public void addAllowedPatterns(String... patterns) {
		obtainAllowListDeserializingConverter().addAllowedPatterns(patterns);
	}

	private AllowListDeserializingConverter obtainAllowListDeserializingConverter() {
		Assert.isTrue(getConverter() instanceof AllowListDeserializingConverter,
				"Patterns can only be provided when using a 'AllowListDeserializingConverter'");
		return (AllowListDeserializingConverter) getConverter();
	}

	@Override
	public String getComponentType() {
		return "deserializing-payload-transformer";
	}

}

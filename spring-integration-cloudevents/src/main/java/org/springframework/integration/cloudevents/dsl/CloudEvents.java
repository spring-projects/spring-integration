/*
 * Copyright 2026-present the original author or authors.
 */

package org.springframework.integration.cloudevents.dsl;

import io.cloudevents.core.format.EventFormat;

import org.springframework.integration.cloudevents.transformer.FromCloudEventTransformer;

/**
 * Factory class for CloudEvents components.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public final class CloudEvents {

	/**
	 * Create a {@link FromCloudEventTransformer}.
	 * @return the {@link FromCloudEventTransformer} instance
	 */
	public static FromCloudEventTransformer fromCloudEventTransformer() {
		return new FromCloudEventTransformer();
	}

	/**
	 * Create a {@link FromCloudEventTransformer} with specified {@link EventFormat}.
	 * @param eventFormat The fallback {@link EventFormat} to use if {@code EventFormatProvider} can not identify the
	 * {@link EventFormat} for the payload.
	 * @return the {@link FromCloudEventTransformer} instance
	 */
	public static FromCloudEventTransformer fromCloudEventTransformer(EventFormat eventFormat) {
		FromCloudEventTransformer transformer = new FromCloudEventTransformer();
		transformer.setEventFormat(eventFormat);
		return transformer;
	}

	/**
	 * Create a {@link ToCloudEventTransformerSpec}.
	 * @return the {@link ToCloudEventTransformerSpec} instance
	 */
	public static ToCloudEventTransformerSpec toCloudEventTransformer() {
		return new ToCloudEventTransformerSpec();
	}

	/**
	 * Create a {@link ToCloudEventTransformerSpec} with extension patterns.
	 * @param extensionPatterns patterns to evaluate whether message headers should be added as extensions
	 * to the {@link io.cloudevents.CloudEvent}
	 * @return the {@link ToCloudEventTransformerSpec} instance
	 */
	public static ToCloudEventTransformerSpec toCloudEventTransformer(String... extensionPatterns) {
		return new ToCloudEventTransformerSpec(extensionPatterns);
	}

	/**
	 * Create a {@link CloudEventHeadersBuilder} with default prefix.
	 * @return the CloudEventHeadersBuilder instance
	 */
	public static CloudEventHeadersBuilder headers() {
		return new CloudEventHeadersBuilder();
	}

	/**
	 * Create a {@link CloudEventHeadersBuilder} with the given prefix.
	 * @param prefix the CloudEvent header prefix
	 * @return the CloudEventHeadersBuilder instance
	 */
	public static CloudEventHeadersBuilder headers(String prefix) {
		return new CloudEventHeadersBuilder(prefix);
	}

	private CloudEvents() {
	}

}

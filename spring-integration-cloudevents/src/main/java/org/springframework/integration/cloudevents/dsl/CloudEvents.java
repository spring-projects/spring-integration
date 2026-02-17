/*
 * Copyright 2026-present the original author or authors.
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
	 * The factory to produce a {@link FromCloudEventTransformer}.
	 * @return the {@link FromCloudEventTransformer} instance
	 */
	public static FromCloudEventTransformer fromCloudEventTransformer() {
		return new FromCloudEventTransformer();
	}

	/**
	 * The factory to produce a {@link FromCloudEventTransformer} with specified {@link EventFormat}.
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
	 * The factory to produce a {@link ToCloudEventTransformerSpec}.
	 * @return the {@link ToCloudEventTransformerSpec} instance
	 */
	public static ToCloudEventTransformerSpec toCloudEventTransformer() {
		return new ToCloudEventTransformerSpec();
	}

	/**
	 * The factory to produce a {@link ToCloudEventTransformerSpec} with extension patterns.
	 * @param extensionPatterns patterns to evaluate whether message headers should be added as extensions
	 *                          to the {@link io.cloudevents.CloudEvent}
	 * @return the {@link ToCloudEventTransformerSpec} instance
	 */
	public static ToCloudEventTransformerSpec toCloudEventTransformer(String... extensionPatterns) {
		return new ToCloudEventTransformerSpec(extensionPatterns);
	}

	private CloudEvents() {
	}

}

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

/**
 * Factory class for CloudEvent components.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public final class CloudEvent {

	/**
	 * The factory to produce a {@link FromCloudEventTransformerSpec}.
	 * @return the {@link FromCloudEventTransformerSpec} instance
	 */
	public static FromCloudEventTransformerSpec fromCloudEventTransformer() {
		return new FromCloudEventTransformerSpec();
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

	private CloudEvent() {
	}
}

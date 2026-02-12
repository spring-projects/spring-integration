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

import java.util.Collections;
import java.util.Map;

import io.cloudevents.core.format.EventFormat;
import io.cloudevents.core.provider.EventFormatProvider;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.cloudevents.transformer.FromCloudEventTransformer;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.messaging.MessageHeaders;

/**
 * A {@link MessageHandlerSpec} for a {@link MessageTransformingHandler} that uses
 * a {@link FromCloudEventTransformer}.
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public class FromCloudEventTransformerSpec
		extends MessageHandlerSpec<FromCloudEventTransformerSpec, MessageTransformingHandler>
		implements ComponentsRegistration {

	private final FromCloudEventTransformer transformer;

	protected FromCloudEventTransformerSpec() {
		this.transformer = new FromCloudEventTransformer();
		this.target = new MessageTransformingHandler(this.transformer);
	}

	/**
	 * Establish the {@link EventFormat} that will be used if the {@link EventFormatProvider} can not identify the
	 * {@link EventFormat} for the {@link MessageHeaders#CONTENT_TYPE} or the message does not contain a
	 * {@link MessageHeaders#CONTENT_TYPE}.
	 * @param eventFormat The fallback {@link EventFormat} to use if {@link EventFormatProvider} can not identify the
	 *                    {@link EventFormat} for the payload.
	 * @return the spec
	 */
	public FromCloudEventTransformerSpec eventFormat(EventFormat eventFormat) {
		this.transformer.setEventFormat(eventFormat);
		return this;
	}

	@Override
	public Map<Object, @Nullable String> getComponentsToRegister() {
		return Collections.singletonMap(this.transformer, null);
	}

}

/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Collections;
import java.util.Map;

import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.messaging.ReactiveMessageHandler;

/**
 * The {@link MessageHandlerSpec} extension for {@link ReactiveMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.4
 */
public abstract class ReactiveMessageHandlerSpec<S extends ReactiveMessageHandlerSpec<S, H>, H extends ReactiveMessageHandler>
		extends MessageHandlerSpec<S, ReactiveMessageHandlerAdapter>
		implements ComponentsRegistration {

	protected final H reactiveMessageHandler; // NOSONAR - final

	protected ReactiveMessageHandlerSpec(H reactiveMessageHandler) {
		this.reactiveMessageHandler = reactiveMessageHandler;
		this.target = new ReactiveMessageHandlerAdapter(this.reactiveMessageHandler);
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		return Collections.singletonMap(this.reactiveMessageHandler, null);
	}

}

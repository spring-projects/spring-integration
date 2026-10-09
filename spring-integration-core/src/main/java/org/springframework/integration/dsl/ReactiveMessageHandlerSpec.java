/*
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.handler.ReactiveMessageHandlerAdapter;
import org.springframework.messaging.ReactiveMessageHandler;

/**
 * The {@link MessageHandlerSpec} extension for {@link ReactiveMessageHandler}.
 *
 * @param <S> the target spec type.
 * @param <H> the target message handler type.
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
	public Map<Object, @Nullable String> getComponentsToRegister() {
		return Collections.<Object, @Nullable String>singletonMap(this.reactiveMessageHandler, null);
	}

}

/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.util.Map;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;

import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.Message;

/**
 *
 * @param <T> the message payload type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
class PublisherIntegrationFlow<T> extends StandardIntegrationFlow implements Publisher<Message<T>> {

	private final Publisher<Message<T>> delegate;

	PublisherIntegrationFlow(Map<Object, String> integrationComponents, Publisher<Message<T>> publisher,
			boolean autoStartOnSubscribe) {

		super(integrationComponents);
		Flux<Message<T>> flux =
				Flux.from(publisher)
						.doOnCancel(this::stop)
						.doOnTerminate(this::stop);

		if (autoStartOnSubscribe) {
			flux = flux.doOnSubscribe((sub) -> start());
			for (Object component : integrationComponents.keySet()) {
				if (component instanceof EndpointSpec<?, ?, ?> endpointSpec) {
					endpointSpec.autoStartup(false);
				}
				else if (component instanceof AbstractEndpoint endpoint) {
					endpoint.setAutoStartup(false);
				}
			}
		}

		this.delegate = flux;

	}

	@Override
	public void subscribe(Subscriber<? super Message<T>> subscriber) {
		this.delegate.subscribe(subscriber);
	}

}

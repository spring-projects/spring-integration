/*
 * Copyright 2016-2022 the original author or authors.
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

import java.util.Map;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;

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
				if (component instanceof EndpointSpec) {
					((EndpointSpec<?, ?, ?>) component).autoStartup(false);
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

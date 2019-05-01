/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.rsocket.inbound;

import java.util.Arrays;

import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.ReactiveMessageHandler;
import org.springframework.util.Assert;

import reactor.core.publisher.Mono;

/**
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketInboundGateway extends MessagingGatewaySupport implements ReactiveMessageHandler {

	private final String[] path;

	public RSocketInboundGateway(String... path) {
		Assert.notNull(path, "'path' must not be null");
		this.path = path;
	}


	@Override
	public Mono<Void> handleMessage(Message<?> message) {
		if (!isRunning()) {
			return Mono.error(new MessageDeliveryException(message,
					"The RSocket Inbound Gateway '" + getComponentName() + "' is stopped; " +
							"service for path " + Arrays.toString(this.path) + " is not available at the moment."));
		}
		return null;
	}

}

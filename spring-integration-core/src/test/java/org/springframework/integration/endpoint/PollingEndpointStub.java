/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.time.Duration;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Jonas Partner
 * @author Gary Russell
 * @author Andreas Baer
 * @author Artem Bilan
 */
public class PollingEndpointStub extends AbstractPollingEndpoint {

	@Override
	protected void onInit() {
		super.onInit();
		setTrigger(new PeriodicTrigger(Duration.ofMillis(500)));
	}

	@Override
	protected void handleMessage(Message<?> message) {
		throw new RuntimeException("intentional test failure");
	}

	@Override
	protected Message<?> receiveMessage() {
		return new GenericMessage<>("test message");
	}

	@Override
	protected Object getResourceToBind() {
		return this;
	}

	@Override
	protected String getResourceKey() {
		return "PollingEndpointStub";
	}

}

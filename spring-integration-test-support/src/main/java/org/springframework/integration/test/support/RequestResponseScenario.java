/*
 * Copyright 2011-2024 the original author or authors.
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

package org.springframework.integration.test.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.Assert;

/**
 * Defines a Spring Integration request response test scenario. All setter methods may
 * be chained.
 * @author David Turanski
 *
 */
public class RequestResponseScenario {

	private final String inputChannelName;

	private final String outputChannelName;

	private Object payload;

	private Message<?> message;

	private AbstractResponseValidator<?> responseValidator;

	private String name;

	protected Message<? extends Object> getMessage() {
		if (message == null) {
			return new GenericMessage<Object>(this.payload);
		}
		else {
			return message;
		}
	}

	/**
	 * Create an instance
	 * @param inputChannelName the input channel name
	 * @param outputChannelName the output channel name
	 */
	public RequestResponseScenario(String inputChannelName, String outputChannelName) {
		this.inputChannelName = inputChannelName;
		this.outputChannelName = outputChannelName;
	}

	/**
	 *
	 * @return the input channel name
	 */
	public String getInputChannelName() {
		return inputChannelName;
	}

	/**
	 *
	 * @return the output channel name
	 */
	public String getOutputChannelName() {
		return outputChannelName;
	}

	/**
	 *
	 * @return the request message payload
	 */
	public Object getPayload() {
		return payload;
	}

	/**
	 * set the payload of the request message
	 * @param payload The payload.
	 * @return this
	 */
	public RequestResponseScenario setPayload(Object payload) {
		this.payload = payload;
		return this;
	}

	/**
	 *
	 * @return the scenario name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the scenario name (optional)
	 * @param name the name
	 * @return this
	 */
	public RequestResponseScenario setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 *
	 * @return the response validator
	 * @see AbstractResponseValidator
	 */
	public AbstractResponseValidator<?> getResponseValidator() {
		return responseValidator;
	}

	/**
	 * Set the response validator
	 * @param responseValidator The response validator.
	 * @return this
	 * @see AbstractResponseValidator
	 */
	public RequestResponseScenario setResponseValidator(AbstractResponseValidator<?> responseValidator) {
		this.responseValidator = responseValidator;
		return this;
	}

	/**
	 * Set the request message (as an alternative to setPayload())
	 * @param message The message.
	 * @return this
	 */
	public RequestResponseScenario setMessage(Message<?> message) {
		this.message = message;
		return this;
	}

	protected void init() {
		Assert.state(message == null || payload == null, "cannot set both message and payload");
	}

}

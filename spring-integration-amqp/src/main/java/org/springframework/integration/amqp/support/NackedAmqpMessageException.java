/*
 * Copyright 2017-present the original author or authors.
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

package org.springframework.integration.amqp.support;

import java.io.Serial;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;

/**
 * An exception representing a negatively acknowledged message from a
 * publisher confirm.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3.12
 *
 */
public class NackedAmqpMessageException extends MessagingException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String nackReason;

	private final transient Object correlationData;

	public NackedAmqpMessageException(Message<?> message, Object correlationData, String nackReason) {
		super(message);
		this.correlationData = correlationData;
		this.nackReason = nackReason;
	}

	public Object getCorrelationData() {
		return this.correlationData;
	}

	public String getNackReason() {
		return this.nackReason;
	}

	@Override
	public String toString() {
		return super.toString() + " [correlationData=" + this.correlationData + ", nackReason=" + this.nackReason
				+ "]";
	}

}

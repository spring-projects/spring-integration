/*
 * Copyright 2018-2019 the original author or authors.
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

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import com.rabbitmq.client.ConfirmCallback;

/**
 * An advice that causes all downstream {@link RabbitOperations} operations to be executed
 * on the same channel, as long as there are no thread handoffs, since the channel is
 * bound to the thread. The same RabbitOperations must be used in this and all downstream
 * components. Typically used with a splitter or some other mechanism that would cause
 * multiple messages to be sent. Optionally waits for publisher confirms if the channel is
 * so configured.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.1
 *
 */
public class BoundRabbitChannelAdvice implements HandleMessageAdvice {

	private final Log logger = LogFactory.getLog(getClass());

	private final RabbitOperations operations;

	private final Duration waitForConfirmsTimeout;

	private final ConfirmCallback ackCallback = this::handleAcks;

	private final ConfirmCallback nackCallback = this::handleNacks;

	/**
	 * Construct an instance that doesn't wait for confirms.
	 * @param operations the operations.
	 */
	public BoundRabbitChannelAdvice(RabbitOperations operations) {
		this(operations, null);
	}

	/**
	 * Construct an instance that waits for publisher confirms (if
	 * configured and waitForConfirmsTimeout is not null).
	 * @param operations the operations.
	 * @param waitForConfirmsTimeout the timeout.
	 */
	public BoundRabbitChannelAdvice(RabbitOperations operations, @Nullable Duration waitForConfirmsTimeout) {
		Assert.notNull(operations, "'operations' cannot be null");
		this.operations = operations;
		this.waitForConfirmsTimeout = waitForConfirmsTimeout;
		if (this.waitForConfirmsTimeout != null) {
			Assert.isTrue(operations.getConnectionFactory().isSimplePublisherConfirms(),
					"'waitForConfirmsTimeout' requires a connection factory with simple publisher confirms enabled");
		}
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		try {
			return this.operations.invoke(operations -> {
				try {
					Object result = invocation.proceed();
					if (this.waitForConfirmsTimeout != null) {
						this.operations.waitForConfirmsOrDie(this.waitForConfirmsTimeout.toMillis());
					}
					return result;
				}
				catch (Throwable t) { // NOSONAR - rethrown below
					ReflectionUtils.rethrowRuntimeException(t);
					return null; // not reachable - satisfy compiler
				}
			}, this.ackCallback, this.nackCallback);
		}
		catch (UndeclaredThrowableException ute) {
			throw ute.getCause();
		}
	}

	private void handleAcks(long deliveryTag, boolean multiple) {
		doHandleAcks(deliveryTag, multiple, true);
	}

	private void handleNacks(long deliveryTag, boolean multiple) {
		doHandleAcks(deliveryTag, multiple, false);
	}

	private void doHandleAcks(long deliveryTag, boolean multiple, boolean ack) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("Publisher confirm " + (!ack ? "n" : "") + "ack: " + deliveryTag + ", " +
					"multiple: " + multiple);
		}
	}

}

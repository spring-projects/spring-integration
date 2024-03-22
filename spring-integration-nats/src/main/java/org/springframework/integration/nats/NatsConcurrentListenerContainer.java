/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.nats.client.MessageHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Container class to support concurrency in NATS Message consumption
 *
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 */
public class NatsConcurrentListenerContainer extends AbstractNatsMessageListenerContainer {

	private static final Log LOG = LogFactory.getLog(NatsConcurrentListenerContainer.class);

	private final List<NatsMessageListenerContainer> containers = new ArrayList<>();

	private int concurrency = 1;

	/**
	 * Construct an instance with the consumerFactory and default delivery mode PULL.
	 *
	 * @param pNatsConsumerFactory NatsConsumerFactory bean with required information to create
	 *     subscription and start polling for message
	 */
	public NatsConcurrentListenerContainer(@NonNull final NatsConsumerFactory pNatsConsumerFactory) {
		super(pNatsConsumerFactory);
	}

	/**
	 * Construct an instance with the consumerFactory and delivery mode
	 *
	 * @param pNatsConsumerFactory NatsConsumerFactory bean with required information to create
	 *     subscription and start polling for message
	 * @param pNatsMessageDeliveryMode option to provide the message delivery mode
	 */
	public NatsConcurrentListenerContainer(
			@NonNull final NatsConsumerFactory pNatsConsumerFactory,
			@NonNull final NatsMessageDeliveryMode pNatsMessageDeliveryMode) {
		super(pNatsConsumerFactory, pNatsMessageDeliveryMode);
	}

	public int getConcurrency() {
		return this.concurrency;
	}

	/**
	 * The maximum number of concurrent {@link NatsMessageListenerContainer}s running.
	 *
	 * @param pConcurrency the concurrency.
	 */
	public void setConcurrency(final int pConcurrency) {
		Assert.isTrue(this.concurrency > 0, "concurrency must be greater than 0");
		this.concurrency = pConcurrency;
	}

	public List<NatsMessageListenerContainer> getContainers() {
		synchronized (this.lifecycleMonitor) {
			return Collections.unmodifiableList(new ArrayList<>(this.containers));
		}
	}

	/** Creates new container based on the concurrency and starts them to start polling */
	@Override
	protected void doStart() {
		if (!isRunning()) {
			setRunning(true);
			final String logMessage =
					"Initiating subscription for subject: "
							+ this.natsConsumerFactory.getConsumerProperties().getSubject()
							+ " and starting NATS consumption with "
							+ this.concurrency
							+ " concurrent consumers using delivery mode: "
							+ this.natsMessageDeliveryMode;
			LOG.info(logMessage);
			for (int i = 0; i < this.concurrency; i++) {
				final NatsMessageListenerContainer container =
						new NatsMessageListenerContainer(
								this.natsConsumerFactory, this.natsMessageDeliveryMode);
				final String beanName = getBeanName();
				container.setBeanName((beanName != null ? beanName : "consumer") + "-" + i);
				container.setApplicationContext(getApplicationContext());
				final MessageHandler messageHandlerLocal = this.messageHandler;
				if (messageHandlerLocal == null) {
					LOG.error("MessageHandler cannot be null");
				}
				else {
					container.setMessageHandler(messageHandlerLocal);
				}
				container.start();
				this.containers.add(container);
			}
		}
	}

	/** Stops the concurrent containers. */
	@Override
	protected void doStop(final Runnable pCallback) {
		if (isRunning()) {
			for (final NatsMessageListenerContainer container : getContainers()) {
				if (container.isRunning()) {
					container.stop(pCallback);
				}
			}
			setRunning(false);
			if (LOG.isDebugEnabled()) {
				LOG.debug(
						"Concurrent Container beans stopped for subject: "
								+ this.natsConsumerFactory.getConsumerProperties().getSubject());
			}
			this.containers.clear();
		}
	}
}

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

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import io.nats.client.support.NatsJetStreamConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static org.springframework.integration.nats.util.RetryablePair.of;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.nats.math.Equations;
import org.springframework.integration.nats.util.NatsUtils;
import org.springframework.integration.nats.util.RetryablePair;
import org.springframework.integration.nats.util.StateChangeLogger;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;

/**
 * NatsMessageAsyncProducingHandler to send nats messages through MessageHandler
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
public class NatsMessageAsyncProducingHandler extends AbstractMessageProducingHandler {

	public static final int DEFAULT_ACK_RECEIVER_MAX_WAIT = 30000;
	private static final Log LOG = LogFactory.getLog(NatsMessageAsyncProducingHandler.class);
	@NonNull
	private final NatsTemplate natsTemplate;
	private final StateChangeLogger changeLogger;

	/**
	 * Buffered concept to store messages for communication between producing and acknowledgment
	 * threads. This buffer as well store the messages during the outage of NATS Server and will help
	 * by short outages.
	 */
	private BlockingQueue<RetryablePair<CompletableFuture<PublishAck>, Message<?>>> publishAckQueue;

	/**
	 * Time period for timeout when 100% {@link
	 * NatsMessageAsyncProducingHandler#ackQueueCapacityAmount} reached and there is no place to
	 * buffer messages on producer side. Default time in milliseconds for awaiting the free space in
	 * blocking queue is 1000 milliseconds
	 */
	private Duration ackQueueCapacityTimeout;

	/**
	 * Time period for timout when producer can't retrieve acknowledgment for sent message to NATS
	 * Server due to different reasons like availability of NATS Server, Network etc. Default time in
	 * milliseconds for awaiting acknowledgment from NATS server
	 */
	private Duration ackNatsServerTimeout;

	/**
	 * Amount of the messages cna be stored in blocking queue for async communication between
	 * producing and ack threads. This amount hast to be monitored and should serve the short outages.
	 * As bigger is amount so longer producer can survive without NATS Server. As good idea is to
	 * monitor threshold e.g. 75%-90% capacity of this value. Default capacity of blocking queue is
	 * 100000
	 */
	private int ackQueueCapacityAmount;

	/**
	 * maximum amount of retry in case when the {@link
	 * NatsMessageAsyncProducingHandler#publishAckQueue} got full if(publishAckQueue.size() == 100%) {
	 * how many times we are going to retry in this case } Default value is 3 times to retry
	 *
	 * <p>max retry by full queue 1. [********* ] 2. [*******************************]
	 */
	private int maxAckFullQueueCapacityAmount; // TODO find better naming

	/**
	 * Time in milliseconds to slow down the {@link AckReceiver} thread when {@link
	 * NatsMessageAsyncProducingHandler#publishAckQueue} is empty
	 */
	private int ackReceiverMaxWait;

	public NatsMessageAsyncProducingHandler(@NonNull final NatsTemplate pNatsTemplate) {
		this.changeLogger = new StateChangeLogger(0);
		ackQueueCapacityAmount = 100000;
		ackQueueCapacityTimeout = Duration.ofMillis(1000);
		ackNatsServerTimeout = Duration.ofMillis(1000);
		maxAckFullQueueCapacityAmount = 3;
		this.natsTemplate = pNatsTemplate;
		this.publishAckQueue = new ArrayBlockingQueue<>(ackQueueCapacityAmount);
		ackReceiverMaxWait = DEFAULT_ACK_RECEIVER_MAX_WAIT;
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new AckReceiver());
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		double occupancy = Equations.percentage(publishAckQueue.size(), ackQueueCapacityAmount);
		// using non-linear dependency to slow down this producer. By reaching
		// 100% of capacity the producer will be slow down for 32 seconds by
		// processing each method
		// By nearing to 100% of capacity the slowness will be bigger and
		// bigger. Only after reaching 75% of threshold the producer begins with
		// slow down for 1 sec.
		final int factor = Equations.binaryBased(occupancy, 75, 5);
		int slownessFactor = 1000 * factor;
		slowDownIfNecessary(slownessFactor);
		// log will be reported only if buffer size reaches at least 75% and
		// will continue report up to 100%.
		// only change will be reported
		this.changeLogger.warnByStateChange(
				factor,
				String.format(
						"NATS AckQueue of Spring Integration Framework reached occupancy=%,.2f%% of capacity!. Slowdown to the factor=%d",
						occupancy, factor));

		Headers headers = NatsUtils.populateNatsMessageHeaders(message);
		final Object payload = message.getPayload();
		// Map header ID from spring integration message to Nats Message Header
		// as MessageID
		String natsMsgId = message.getHeaders().getId().toString();
		headers.add(NatsJetStreamConstants.MSG_ID_HDR, natsMsgId);

		if (payload != null) {
			try {
				LOG.debug(
						"Trying asynchronously to publish the message: "
								+ payload
								+ " to subject: "
								+ this.natsTemplate.getSubject());
				final CompletableFuture<PublishAck> publishAck =
						this.natsTemplate.sendAsync(payload, headers);
				boolean success =
						publishAckQueue.offer(
								of(publishAck, message), ackQueueCapacityTimeout.toMillis(), TimeUnit.MILLISECONDS);
				if (!success) {
					LOG.info(
							"I had an issue to offer the message to publishAckQueue of BlockingQueue. It could be due to properties org.springframework.integration.nats.producer.ack.capacity and org.springframework.integration.nats.producer.ack.capacity.timeout.mls."
									+ "Please consider to tune one of the values.");
					// Send messages into reply channel. To get infinite retry
					// logic set inputChannel=replyChannel, because the message
					// will be polled again from inputChannel.
					// That is how the redelivery works
					sendOutput(message, null, false);
				}
			}
			catch (IOException | JetStreamApiException e) {
				throw new MessageDeliveryException(
						message,
						"Exception occurred while sending message to " + this.natsTemplate.getSubject(),
						e);
			}
			catch (InterruptedException e) {
				LOG.info(
						"Acknowledgment capacity is exceptionally exceeded due to experimental setting of org.springframework.integration.nats.producer.ack.capacity and org.springframework.integration.nats.producer.ack.capacity.timeout.mls."
								+ "Please consider to increase one of the values.");
			}
		}
	}

	/**
	 * slowdowns main producer thread if necessary when value is greater then 0
	 *
	 * @param value - TODO: Add description
	 */
	private void slowDownIfNecessary(int value) {
		if (value > 0) {
			try {
				Thread.sleep(value);
			}
			catch (InterruptedException e) {
				LOG.debug("Sleep was interrupted.");
			}
		}
	}

	public Duration getAckQueueCapacityTimeout() {
		return ackQueueCapacityTimeout;
	}

	public void setAckQueueCapacityTimeout(Duration ackQueueCapacityTimeout) {
		this.ackQueueCapacityTimeout = ackQueueCapacityTimeout;
	}

	public Duration getAckNatsServerTimeout() {
		return ackNatsServerTimeout;
	}

	public void setAckNatsServerTimeout(Duration ackNatsServerTimeout) {
		this.ackNatsServerTimeout = ackNatsServerTimeout;
	}

	public int getAckQueueCapacityAmount() {
		return ackQueueCapacityAmount;
	}

	public void setAckQueueCapacityAmount(int ackQueueCapacityAmount) {
		this.ackQueueCapacityAmount = ackQueueCapacityAmount;
		this.publishAckQueue = new ArrayBlockingQueue<>(this.ackQueueCapacityAmount);
	}

	public void setMaxAckFullQueueCapacityAmount(int maxAckFullQueueCapacityAmount) {
		this.maxAckFullQueueCapacityAmount = maxAckFullQueueCapacityAmount;
	}

	public void setAckReceiverMaxWait(int ackReceiverMaxWait) {
		this.ackReceiverMaxWait = ackReceiverMaxWait;
	}

	/**
	 * Receiver which responsible to span separated single thread differ from the thread which
	 * responsible for publishing the messages to NATS Server TODO: consider idee to slow down the
	 * receiver thread when the capacity of {@link
	 * NatsMessageAsyncProducingHandler#ackQueueCapacityAmount} reached the threshold value e.g. 75%
	 * and more slowdown when capacity reached 90%
	 */
	private final class AckReceiver implements Runnable {

		@Override
		public void run() {
			LOG.info(
					"Acknowledgment receiver is started within tread: " + Thread.currentThread().getName());
			while (true) {
				// If Queue is empty, slow down peeking of this thread by 30
				// seconds( configurable later)
				if (publishAckQueue.isEmpty()) {
					slowDownIfNecessary(ackReceiverMaxWait);
				}
				else {
					RetryablePair<CompletableFuture<PublishAck>, Message<?>> ackPair = null;
					ackPair = publishAckQueue.peek();
					CompletableFuture<PublishAck> pubAck = ackPair.getFirst();
					Message<?> message = ackPair.getSecond();
					try {
						PublishAck publishAck =
								pubAck.get(ackNatsServerTimeout.toMillis(), TimeUnit.MILLISECONDS);
						if (publishAck != null) {
							LOG.debug(
									"Nats Message acknowledgment received: "
											+ message.getPayload()
											+ " "
											+ publishAck);
							publishAckQueue.remove(ackPair);
						}
						else {
							handleUnpleasantAck(ackPair, pubAck, message);
						}
					}
					catch (TimeoutException e) {
						LOG.warn(
								"Timeout occurred by delivery of acknowledgment for the following message "
										+ message.getPayload()
										+ " "
										+ pubAck,
								e);
					}
					catch (ExecutionException | InterruptedException | CancellationException e) {
						LOG.warn(
								"Execution or interrupt or cancel exception occurred for acknowledgment with the following message "
										+ message.getPayload()
										+ " "
										+ pubAck,
								e);
						// Send messages into reply channel. To get infinite
						// retry logic set inputChannel=replyChannel, because
						// the message will be polled again from inputChannel.
						// That is how the redelivery works.
						publishAckQueue.remove(ackPair);
						sendOutput(message, null, false);
					}
				}
			}
		}

		private void handleUnpleasantAck(
				RetryablePair<CompletableFuture<PublishAck>, Message<?>> ackPair,
				CompletableFuture<PublishAck> pubAck,
				Message<?> message) {
			if (ackPair.increment() > maxAckFullQueueCapacityAmount) {
				publishAckQueue.remove(ackPair);
				sendOutput(message, null, false);
			}
		}
	}
}

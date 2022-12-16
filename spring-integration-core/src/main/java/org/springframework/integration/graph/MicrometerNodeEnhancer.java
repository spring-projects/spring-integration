/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.integration.graph;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.observation.DefaultMessageReceiverObservationConvention;
import org.springframework.integration.support.management.observation.DefaultMessageRequestReplyReceiverObservationConvention;
import org.springframework.integration.support.management.observation.DefaultMessageSenderObservationConvention;
import org.springframework.integration.support.management.observation.IntegrationObservation;
import org.springframework.lang.Nullable;

/**
 * Add micrometer metrics to the node.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
public class MicrometerNodeEnhancer {

	private static final String UNUSED = "unused";

	private static final String TAG_TYPE = "type";

	private static final String TAG_NAME = "name";

	private static final String TAG_RESULT = "result";

	private static final TimerStats ZERO_TIMER_STATS = new TimerStats(0L, 0.0, 0.0);

	private final MeterRegistry registry;

	MicrometerNodeEnhancer(ApplicationContext applicationContext) {
		Map<String, MeterRegistry> registries = applicationContext.getBeansOfType(MeterRegistry.class, false, false);
		if (registries.size() == 1) {
			this.registry = registries.values().iterator().next();
		}
		else {
			this.registry = null;
		}
	}

	/**
	 * Add micrometer metrics to the node.
	 * @param <T> the node type.
	 * @param node the node.
	 * @return the enhanced node.
	 */
	<T extends IntegrationNode> T enhance(T node) {
		if (this.registry != null) {
			if (node instanceof MessageChannelNode) {
				enhanceWithTimers(node, "channel");
			}
			else if (node instanceof MessageHandlerNode || node instanceof MessageProducerNode) {
				enhanceWithTimers(node, "handler");
			}
			if (node instanceof PollableChannelNode) {
				enhanceWithCounts(node, "channel");
			}
			else if (node instanceof MessageSourceNode) {
				enhanceWithCounts(node, "source");
			}
			else if (node instanceof MessageGatewayNode) {
				enhanceWithTimers(node, "gateway");
			}
		}
		return node;
	}

	private <T extends IntegrationNode> void enhanceWithTimers(T node, String type) {
		((SendTimersAware) node).sendTimers(() -> retrieveTimers(node, type));
	}

	private <T extends IntegrationNode> SendTimers retrieveTimers(T node, String type) {
		Timer successTimer = obtainTimer(node, type, true);
		Timer failureTimer = obtainTimer(node, type, false);

		return new SendTimers(buildTimerStats(successTimer), buildTimerStats(failureTimer));
	}

	@Nullable
	private <T extends IntegrationNode> Timer obtainTimer(T node, String type, boolean success) {
		try {
			if (node.isObserved()) {
				return observationTimer(node, type, success);
			}
			else {
				return this.registry.get(IntegrationManagement.SEND_TIMER_NAME)
						.tag(TAG_TYPE, type)
						.tag(TAG_NAME, node.getName())
						.tag(TAG_RESULT, success ? "success" : "failure")
						.timer();
			}
		}
		catch (Exception ex) {
			return null;
		}
	}

	@Nullable
	private <T extends IntegrationNode> Timer observationTimer(T node, String type, boolean success) {
		Search timerSearch =
				switch (type) {
					case "channel" -> this.registry.find(DefaultMessageSenderObservationConvention.INSTANCE.getName())
							.tag(IntegrationObservation.ProducerTags.COMPONENT_TYPE.asString(), "producer");
					case "handler" -> this.registry.find(DefaultMessageReceiverObservationConvention.INSTANCE.getName())
							.tag(IntegrationObservation.HandlerTags.COMPONENT_TYPE.asString(), "handler");
					case "gateway" ->
							this.registry.find(DefaultMessageRequestReplyReceiverObservationConvention.INSTANCE.getName())
									.tag(IntegrationObservation.GatewayTags.COMPONENT_TYPE.asString(), "gateway");
					default -> null;
				};

		if (timerSearch != null) {
			return timerSearch
					.tag(IntegrationObservation.HandlerTags.COMPONENT_NAME.asString(), node.getName())
					.tag("error", value -> success == "none".equals(value))
					.timer();
		}

		return null;
	}

	private <T extends IntegrationNode> void enhanceWithCounts(T node, String type) {
		((ReceiveCountersAware) node).receiveCounters(() -> retrieveCounters(node, type));
	}

	private <T extends IntegrationNode> ReceiveCounters retrieveCounters(T node, String type) {
		Counter successes = null;
		String name = node.getName();
		try {
			successes = this.registry.get(IntegrationManagement.RECEIVE_COUNTER_NAME)
					.tag(TAG_TYPE, type)
					.tag(TAG_NAME, name)
					.tag(TAG_RESULT, "success")
					.counter();
		}
		catch (@SuppressWarnings(UNUSED) Exception e) { // NOSONAR ignored
			// NOSONAR empty;
		}
		Counter failures = null;
		try {
			failures = this.registry.get(IntegrationManagement.RECEIVE_COUNTER_NAME)
					.tag(TAG_TYPE, type)
					.tag(TAG_NAME, name)
					.tag(TAG_RESULT, "failure")
					.counter();
		}
		catch (@SuppressWarnings(UNUSED) Exception e) { // NOSONAR ignored
			// NOSONAR empty;
		}
		return new ReceiveCounters(
				(long) (successes == null ? 0 : successes.count()),
				(long) (failures == null ? 0 : failures.count()));
	}

	private static TimerStats buildTimerStats(@Nullable Timer timer) {
		return timer == null
				? ZERO_TIMER_STATS
				: new TimerStats(timer.count(), timer.mean(TimeUnit.MILLISECONDS), timer.max(TimeUnit.MILLISECONDS));
	}

}


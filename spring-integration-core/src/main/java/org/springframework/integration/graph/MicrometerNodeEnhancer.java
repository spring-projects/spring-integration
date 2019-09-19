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

package org.springframework.integration.graph;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.management.IntegrationManagement;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Add micrometer metrics to the node.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class MicrometerNodeEnhancer {

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
			else if (node instanceof MessageHandlerNode) {
				enhanceWithTimers(node, "handler");
			}
			if (node instanceof PollableChannelNode) {
				enhanceWithCounts(node, "channel");
			}
			else if (node instanceof MessageSourceNode) {
				enhanceWithCounts(node, "source");
			}
		}
		return node;
	}

	private <T extends IntegrationNode> void enhanceWithTimers(T node, String type) {
		Timer successTimer = null;
		try {
			successTimer = this.registry.get(IntegrationManagement.SEND_TIMER_NAME)
					.tag("type", type)
					.tag("name", node.getName())
					.tag("result", "success")
					.timer();
		}
		catch (@SuppressWarnings("unused")
		Exception e) {
			// NOSONAR empty
		}
		Timer failureTimer = null;
		try {
			failureTimer = this.registry.get(IntegrationManagement.SEND_TIMER_NAME)
					.tag("type", type)
					.tag("name", node.getName())
					.tag("result", "failure")
					.timer();
		}
		catch (@SuppressWarnings("unused")
		Exception e) {
			// NOSONAR empty;
		}
		TimerStats successes = successTimer == null ? ZERO_TIMER_STATS
				: new TimerStats(successTimer.count(), successTimer.mean(TimeUnit.MILLISECONDS),
						successTimer.max(TimeUnit.MILLISECONDS));
		TimerStats failures = failureTimer == null ? ZERO_TIMER_STATS
				: new TimerStats(failureTimer.count(), failureTimer.mean(TimeUnit.MILLISECONDS),
						failureTimer.max(TimeUnit.MILLISECONDS));
		((SendTimersAware) node).sendTimers(new SendTimers(successes, failures));
	}

	private <T extends IntegrationNode> void enhanceWithCounts(T node, String type) {
		Counter successes = null;
		String name = node.getName();
		try {
			successes = this.registry.get(IntegrationManagement.RECEIVE_COUNTER_NAME)
					.tag("type", type)
					.tag("name", name)
					.tag("result", "success")
					.counter();
		}
		catch (@SuppressWarnings("unused") Exception e) {
			// NOSONAR empty;
		}
		Counter failures = null;
		try {
			failures = this.registry.get(IntegrationManagement.RECEIVE_COUNTER_NAME)
				.tag("type", type)
				.tag("name", name)
				.tag("result", "failure")
				.counter();
		}
		catch (@SuppressWarnings("unused") Exception e) {
			// NOSONAR empty;
		}
		((ReceiveCountersAware) node)
				.receiveCounters(new ReceiveCounters(
						(long) (successes == null ? 0 : successes.count()),
						(long) (failures == null ? 0 : failures.count())));
	}

}


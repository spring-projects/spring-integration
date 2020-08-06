/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.integration.handler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.Ordered;
import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.integration.support.management.metrics.MeterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.support.management.metrics.TimerFacade;

/**
 * Base class for Message handling components that provides basic validation and error
 * handling capabilities. Asserts that the incoming Message is not null and that it does
 * not contain a null payload. Converts checked exceptions into runtime
 * {@link org.springframework.messaging.MessagingException}s.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Amit Sadafule
 * @author David Turanski
 *
 * @since 5.3
 *
 */
@IntegrationManagedResource
public abstract class MessageHandlerSupport extends IntegrationObjectSupport
		implements TrackableComponent, Orderable, IntegrationManagement, IntegrationPattern {

	private final ManagementOverrides managementOverrides = new ManagementOverrides();

	private final Set<TimerFacade> timers = ConcurrentHashMap.newKeySet();

	private boolean shouldTrack = false;

	private boolean loggingEnabled = true;

	private MetricsCaptor metricsCaptor;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private String managedName;

	private String managedType;

	private TimerFacade successTimer;

	@Override
	public boolean isLoggingEnabled() {
		return this.loggingEnabled;
	}

	@Override
	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
		this.managementOverrides.loggingConfigured = true;
	}

	@Override
	public void registerMetricsCaptor(MetricsCaptor metricsCaptorToRegister) {
		this.metricsCaptor = metricsCaptorToRegister;
	}

	protected MetricsCaptor getMetricsCaptor() {
		return this.metricsCaptor;
	}

	@Override
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public String getComponentType() {
		return "message-handler";
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	protected boolean shouldTrack() {
		return this.shouldTrack;
	}

	@Override
	public ManagementOverrides getOverrides() {
		return this.managementOverrides;
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.outbound_channel_adapter;
	}

	protected TimerFacade sendTimer() {
		if (this.successTimer == null) {
			this.successTimer = buildSendTimer(true, "none");
		}
		return this.successTimer;
	}

	protected TimerFacade buildSendTimer(boolean success, String exception) {
		TimerFacade timer = this.metricsCaptor.timerBuilder(SEND_TIMER_NAME)
				.tag("type", "handler")
				.tag("name", getComponentName() == null ? "unknown" : getComponentName())
				.tag("result", success ? "success" : "failure")
				.tag("exception", exception)
				.description("Send processing time")
				.build();
		this.timers.add(timer);
		return timer;
	}

	public void setManagedName(String managedName) {
		this.managedName = managedName;
	}

	public String getManagedName() {
		return this.managedName;
	}

	public void setManagedType(String managedType) {
		this.managedType = managedType;
	}

	public String getManagedType() {
		return this.managedType;
	}

	@Override
	public void destroy() {
		this.timers.forEach(MeterFacade::remove);
		this.timers.clear();
	}

}

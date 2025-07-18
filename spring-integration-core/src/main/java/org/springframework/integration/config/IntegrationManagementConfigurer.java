/*
 * Copyright 2015-present the original author or authors.
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

package org.springframework.integration.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.IntegrationManagement.ManagementOverrides;
import org.springframework.integration.support.management.metrics.MeterFacade;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * Configures beans that implement {@link IntegrationManagement}.
 * Configures logging, {@link MetricsCaptor} and {@link ObservationRegistry} for all (or selected) components.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Meherzad Lahewala
 * @author Jonathan Pearlin
 *
 * @since 4.2
 *
 */
public class IntegrationManagementConfigurer
		implements SmartInitializingSingleton, ApplicationContextAware, BeanNameAware, BeanPostProcessor,
		ApplicationListener<ContextClosedEvent> {

	/**
	 * Bean name of the configurer.
	 */
	public static final String MANAGEMENT_CONFIGURER_NAME = "integrationManagementConfigurer";

	private final Set<MeterFacade> gauges = new HashSet<>();

	@SuppressWarnings("NullAway.Init")
	private ApplicationContext applicationContext;

	@SuppressWarnings("NullAway.Init")
	private String beanName;

	private boolean defaultLoggingEnabled = true;

	private volatile boolean singletonsInstantiated;

	private @Nullable MetricsCaptor metricsCaptor;

	private @Nullable ObjectProvider<MetricsCaptor> metricsCaptorProvider;

	private @Nullable ObservationRegistry observationRegistry;

	private @Nullable ObjectProvider<ObservationRegistry> observationRegistryProvider;

	@SuppressWarnings("NullAway.Init") //Creation is handled in the postProcessAfterInitialization
	private String[]  observationPatterns;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * Disable all logging in the normal message flow in framework components. When 'false', such logging will be
	 * skipped, regardless of logging level. When 'true', the logging is controlled as normal by the logging
	 * subsystem log level configuration.
	 * <p>
	 * Exception logging (debug or otherwise) is not affected by this setting.
	 * <p>
	 * It has been found that in high-volume messaging environments, calls to methods such as
	 * {@link Log#isDebugEnabled()} can be quite expensive
	 * and account for an inordinate amount of CPU time.
	 * <p>
	 * Set this to 'false' to disable logging by default in all framework components that implement
	 * {@link IntegrationManagement} (channels, message handlers etc.) This turns off logging such as
	 * "PreSend on channel", "Received message" etc.
	 * <p>
	 * After the context is initialized, individual components can have their setting changed by invoking
	 * {@link IntegrationManagement#setLoggingEnabled(boolean)}.
	 * @param defaultLoggingEnabled defaults to true.
	 */
	public void setDefaultLoggingEnabled(boolean defaultLoggingEnabled) {
		this.defaultLoggingEnabled = defaultLoggingEnabled;
	}

	public void setMetricsCaptor(@Nullable MetricsCaptor metricsCaptor) {
		this.metricsCaptor = metricsCaptor;
	}

	void setMetricsCaptorProvider(ObjectProvider<MetricsCaptor> metricsCaptorProvider) {
		this.metricsCaptorProvider = metricsCaptorProvider;
	}

	/**
	 * Set an {@link ObservationRegistry} to populate to the {@link IntegrationManagement} components
	 * in the application context.
	 * @param observationRegistry the {@link ObservationRegistry} to use.
	 * @since 6.0
	 */
	public void setObservationRegistry(@Nullable ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	void setObservationRegistry(ObjectProvider<ObservationRegistry> observationRegistryProvider) {
		this.observationRegistryProvider = observationRegistryProvider;
	}

	/**
	 * Set simple patterns for component names matching which has to be instrumented with a {@link ObservationRegistry}.
	 * @param observationPatterns the simple patterns to use.
	 * @since 6.0
	 * @see PatternMatchUtils#smartMatch(String, String...)
	 */
	public void setObservationPatterns(String... observationPatterns) {
		Assert.notEmpty(observationPatterns, "'observationPatterns' must not be empty");
		this.observationPatterns = Arrays.copyOf(observationPatterns, observationPatterns.length);
	}

	@Override
	public void afterSingletonsInstantiated() {
		Assert.state(this.applicationContext != null, "'applicationContext' must not be null");
		Assert.state(MANAGEMENT_CONFIGURER_NAME.equals(this.beanName), getClass().getSimpleName()
				+ " bean name must be " + MANAGEMENT_CONFIGURER_NAME);

		MetricsCaptor metricsCaptorToUse = obtainMetricsCaptor();
		if (metricsCaptorToUse != null) {
			registerComponentGauges(metricsCaptorToUse);
		}

		setupObservationRegistry();

		this.applicationContext.getBeansOfType(IntegrationManagement.class).values()
				.forEach(this::enhanceIntegrationManagement);

		this.singletonsInstantiated = true;
	}

	@Nullable
	private MetricsCaptor obtainMetricsCaptor() {
		if (this.metricsCaptor == null && this.metricsCaptorProvider != null) {
			this.metricsCaptor = this.metricsCaptorProvider.getIfUnique();
		}
		return this.metricsCaptor;
	}

	private void setupObservationRegistry() {
		if (this.observationRegistry == null && this.observationRegistryProvider != null) {
			this.observationRegistry = this.observationRegistryProvider.getIfUnique();
		}
	}

	private void registerComponentGauges(MetricsCaptor metricsCaptor) {
		this.gauges.add(
				metricsCaptor.gaugeBuilder("spring.integration.channels", this,
								(c) -> this.applicationContext.getBeansOfType(MessageChannel.class).size())
						.description("The number of message channels")
						.build());

		this.gauges.add(
				metricsCaptor.gaugeBuilder("spring.integration.handlers", this,
								(c) -> this.applicationContext.getBeansOfType(MessageHandler.class).size())
						.description("The number of message handlers")
						.build());

		this.gauges.add(
				metricsCaptor.gaugeBuilder("spring.integration.sources", this,
								(c) -> this.applicationContext.getBeansOfType(MessageSource.class).size())
						.description("The number of message sources")
						.build());
	}

	private void enhanceIntegrationManagement(IntegrationManagement integrationManagement) {
		if (!getOverrides(integrationManagement).loggingConfigured) {
			integrationManagement.setLoggingEnabled(this.defaultLoggingEnabled);
		}
		if (this.metricsCaptor != null) {
			integrationManagement.registerMetricsCaptor(this.metricsCaptor);
		}
		if (this.observationRegistry != null &&
				Boolean.TRUE.equals(PatternMatchUtils.smartMatch(
						integrationManagement.getComponentName(), this.observationPatterns))) {

			integrationManagement.registerObservationRegistry(this.observationRegistry);
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
		if (this.singletonsInstantiated && bean instanceof IntegrationManagement integrationManagement) {
			enhanceIntegrationManagement(integrationManagement);
		}
		return bean;
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		if (event.getApplicationContext().equals(this.applicationContext)) {
			this.gauges.forEach(MeterFacade::remove);
			this.gauges.clear();
		}
	}

	private static ManagementOverrides getOverrides(IntegrationManagement bean) {
		return bean.getOverrides() != null ? bean.getOverrides() : new ManagementOverrides();
	}

}

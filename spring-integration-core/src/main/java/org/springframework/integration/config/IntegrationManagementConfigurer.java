/*
 * Copyright 2015-2020 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.IntegrationManagement.ManagementOverrides;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.integration.support.management.micrometer.MicrometerMetricsCaptor;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;


/**
 * Configures beans that implement {@link IntegrationManagement}.
 * Configures counts, stats, logging for all (or selected) components.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Meherzad Lahewala
 * @author Jonathan Pearlin
 *
 * @since 4.2
 *
 */
@SuppressWarnings("deprecation")
public class IntegrationManagementConfigurer
		implements SmartInitializingSingleton, ApplicationContextAware, BeanNameAware,
		DestructionAwareBeanPostProcessor {

	private static final Log LOGGER = LogFactory.getLog(IntegrationManagementConfigurer.class);

	public static final String MANAGEMENT_CONFIGURER_NAME = "integrationManagementConfigurer";

	private final Map<String, org.springframework.integration.support.management.MessageChannelMetrics>
			channelsByName = new HashMap<>();

	private final Map<String, org.springframework.integration.support.management.MessageHandlerMetrics>
			handlersByName = new HashMap<>();

	private final Map<String, org.springframework.integration.support.management.MessageSourceMetrics>
			sourcesByName = new HashMap<>();

	private final Map<String,
			org.springframework.integration.support.management.MessageSourceMetricsConfigurer>
			sourceConfigurers = new HashMap<>();

	private ApplicationContext applicationContext;

	private String beanName;

	private boolean defaultLoggingEnabled = true;

	private Boolean defaultCountsEnabled = false;

	private Boolean defaultStatsEnabled = false;

	private org.springframework.integration.support.management.MetricsFactory metricsFactory;

	private String metricsFactoryBeanName;

	private String[] enabledCountsPatterns = { };

	private String[] enabledStatsPatterns = { };

	private volatile boolean singletonsInstantiated;

	private MetricsCaptor metricsCaptor;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	/**
	 * Set a metrics factory.
	 * Has a precedence over {@link #metricsFactoryBeanName}.
	 * Defaults to {@link org.springframework.integration.support.management.DefaultMetricsFactory}.
	 * @deprecated in favor of dimensional metrics via
	 * {@link org.springframework.integration.support.management.metrics.MeterFacade}.
	 * Built-in metrics will be removed in a future release.
	 * @param metricsFactory the factory.
	 * @since 4.2
	 */
	@Deprecated
	public void setMetricsFactory(org.springframework.integration.support.management.MetricsFactory metricsFactory) {
		this.metricsFactory = metricsFactory;
	}

	/**
	 * Set a metrics factory bean name.
	 * Is used if {@link #metricsFactory} isn't specified.
	 * @param metricsFactory the factory.
	 * @deprecated in favor of dimensional metrics via
	 * {@link org.springframework.integration.support.management.metrics.MeterFacade}.
	 * Built-in metrics will be removed in a future release.
	 * @since 4.2
	 */
	@Deprecated
	public void setMetricsFactoryBeanName(String metricsFactory) {
		this.metricsFactoryBeanName = metricsFactory;
	}

	/**
	 * Set the array of simple patterns for component names for which message counts will
	 * be enabled (defaults to '*').
	 * Enables message counting (`sendCount`, `errorCount`, `receiveCount`)
	 * for those components that support counters (channels, message handlers, etc).
	 * This is the initial setting only, individual components can have counts
	 * enabled/disabled at runtime. May be overridden by an entry in
	 * {@link #setEnabledStatsPatterns(String[]) enabledStatsPatterns} which is additional
	 * functionality over simple counts. If a pattern starts with `!`, counts are disabled
	 * for matches. For components that match multiple patterns, the first pattern wins.
	 * Disabling counts at runtime also disables stats.
	 * @param enabledCountsPatterns the patterns.
	 */
	public void setEnabledCountsPatterns(String[] enabledCountsPatterns) {
		Assert.notEmpty(enabledCountsPatterns, "enabledCountsPatterns must not be empty");
		this.enabledCountsPatterns = Arrays.copyOf(enabledCountsPatterns, enabledCountsPatterns.length);
	}

	/**
	 * Set the array of simple patterns for component names for which message statistics
	 * will be enabled (response times, rates etc), as well as counts (a positive match
	 * here overrides {@link #setEnabledCountsPatterns(String[]) enabledCountsPatterns},
	 * you can't have statistics without counts). (defaults to '*').
	 * Enables statistics for those components that support statistics
	 * (channels - when sending, message handlers, etc). This is the initial setting only,
	 * individual components can have stats enabled/disabled at runtime. If a pattern
	 * starts with `!`, stats (and counts) are disabled for matches. Note: this means that
	 * '!foo' here will disable stats and counts for 'foo' even if counts are enabled for
	 * 'foo' in {@link #setEnabledCountsPatterns(String[]) enabledCountsPatterns}. For
	 * components that match multiple patterns, the first pattern wins. Enabling stats at
	 * runtime also enables counts.
	 * @deprecated in favor of dimensional metrics via
	 * {@link org.springframework.integration.support.management.metrics.MeterFacade}.
	 * Built-in metrics will be removed in a future release.
	 * @param enabledStatsPatterns the patterns.
	 */
	@Deprecated
	public void setEnabledStatsPatterns(String[] enabledStatsPatterns) {
		Assert.notEmpty(enabledStatsPatterns, "enabledStatsPatterns must not be empty");
		this.enabledStatsPatterns = Arrays.copyOf(enabledStatsPatterns, enabledStatsPatterns.length);
	}

	/**
	 * Set whether managed components maintain message counts by default.
	 * Defaults to false, unless an Integration MBean Exporter is configured.
	 * @param defaultCountsEnabled true to enable.
	 */
	public void setDefaultCountsEnabled(Boolean defaultCountsEnabled) {
		this.defaultCountsEnabled = defaultCountsEnabled;
	}

	public Boolean getDefaultCountsEnabled() {
		return this.defaultCountsEnabled;
	}

	/**
	 * Set whether managed components maintain message statistics by default.
	 * Defaults to false, unless an Integration MBean Exporter is configured.
	 * @param defaultStatsEnabled true to enable.
	 * @deprecated in favor of dimensional metrics via
	 * {@link org.springframework.integration.support.management.metrics.MeterFacade}.
	 * Built-in metrics will be removed in a future release.
	 */
	@Deprecated
	public void setDefaultStatsEnabled(Boolean defaultStatsEnabled) {
		this.defaultStatsEnabled = defaultStatsEnabled;
	}

	/**
	 * Return true if stats are enabled by default.
	 * @return the stats enabled.
	 * @deprecated in favor of dimensional metrics via
	 * {@link org.springframework.integration.support.management.metrics.MeterFacade}.
	 * Built-in metrics will be removed in a future release.
	 */
	@Deprecated
	public Boolean getDefaultStatsEnabled() {
		return this.defaultStatsEnabled;
	}

	/**
	 * Disable all logging in the normal message flow in framework components. When 'false', such logging will be
	 * skipped, regardless of logging level. When 'true', the logging is controlled as normal by the logging
	 * subsystem log level configuration.
	 * <p>
	 * Exception logging (debug or otherwise) is not affected by this setting.
	 * <p>
	 * It has been found that in high-volume messaging environments, calls to methods such as
	 * {@link Log#isDebugEnabled()} can be quite expensive and account for an inordinate amount of CPU
	 * time.
	 * <p>
	 * Set this to false to disable logging by default in all framework components that implement
	 * {@link IntegrationManagement} (channels, message handlers etc). This turns off logging such as
	 * "PreSend on channel", "Received message" etc.
	 * <p>
	 * After the context is initialized, individual components can have their setting changed by invoking
	 * {@link IntegrationManagement#setLoggingEnabled(boolean)}.
	 * @param defaultLoggingEnabled defaults to true.
	 */
	public void setDefaultLoggingEnabled(boolean defaultLoggingEnabled) {
		this.defaultLoggingEnabled = defaultLoggingEnabled;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Assert.state(this.applicationContext != null, "'applicationContext' must not be null");
		Assert.state(MANAGEMENT_CONFIGURER_NAME.equals(this.beanName), getClass().getSimpleName()
				+ " bean name must be " + MANAGEMENT_CONFIGURER_NAME);
		if (ClassUtils.isPresent("io.micrometer.core.instrument.MeterRegistry",
				this.applicationContext.getClassLoader())) {
			this.metricsCaptor = MicrometerMetricsCaptor.loadCaptor(this.applicationContext);
		}
		if (this.metricsCaptor != null) {
			injectCaptor();
			registerComponentGauges();
		}
		if (this.metricsFactory == null && StringUtils.hasText(this.metricsFactoryBeanName)) {
			this.metricsFactory = this.applicationContext.getBean(this.metricsFactoryBeanName,
					org.springframework.integration.support.management.MetricsFactory.class);
		}
		if (this.metricsFactory == null) {
			Map<String, org.springframework.integration.support.management.MetricsFactory>
					factories = this.applicationContext
					.getBeansOfType(org.springframework.integration.support.management.MetricsFactory.class);
			if (factories.size() == 1) {
				this.metricsFactory = factories.values().iterator().next();
			}
		}
		if (this.metricsFactory == null) {
			this.metricsFactory = new org.springframework.integration.support.management.DefaultMetricsFactory();
		}
		this.sourceConfigurers.putAll(this.applicationContext.getBeansOfType(
				org.springframework.integration.support.management.MessageSourceMetricsConfigurer.class));
		Map<String, IntegrationManagement> managed = this.applicationContext
				.getBeansOfType(IntegrationManagement.class);
		for (Entry<String, IntegrationManagement> entry : managed.entrySet()) {
			IntegrationManagement bean = entry.getValue();
			if (!getOverrides(bean).loggingConfigured) {
				bean.setLoggingEnabled(this.defaultLoggingEnabled);
			}
			String name = entry.getKey();
			doConfigureMetrics(bean, name);
		}
		this.singletonsInstantiated = true;
	}

	private void injectCaptor() {
		Map<String, IntegrationManagement> managed = this.applicationContext
				.getBeansOfType(IntegrationManagement.class);
		for (Entry<String, IntegrationManagement> entry : managed.entrySet()) {
			IntegrationManagement bean = entry.getValue();
			if (!getOverrides(bean).loggingConfigured) {
				bean.setLoggingEnabled(this.defaultLoggingEnabled);
			}
			bean.registerMetricsCaptor(this.metricsCaptor);
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
		if (this.singletonsInstantiated) {
			if (bean instanceof IntegrationManagement) {
				((IntegrationManagement) bean).registerMetricsCaptor(this.metricsCaptor);
			}
			return doConfigureMetrics(bean, name);
		}
		return bean;
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return bean instanceof org.springframework.integration.support.management.MessageChannelMetrics ||
				bean instanceof org.springframework.integration.support.management.MessageHandlerMetrics ||
				bean instanceof org.springframework.integration.support.management.MessageSourceMetrics;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String nameOfBean) throws BeansException {
		if (bean instanceof org.springframework.integration.support.management.MessageChannelMetrics) {
			this.channelsByName.remove(nameOfBean);
		}
		else if (bean instanceof org.springframework.integration.support.management.MessageHandlerMetrics) {
			if (this.handlersByName.remove(nameOfBean) == null) {
				this.handlersByName.remove(nameOfBean + ".handler");
			}
		}
		else if (bean instanceof org.springframework.integration.support.management.MessageSourceMetrics &&
				this.sourcesByName.remove(nameOfBean) == null) {

			this.sourcesByName.remove(nameOfBean + ".source");
		}
	}

	private Object doConfigureMetrics(Object bean, String name) {
		if (bean instanceof org.springframework.integration.support.management.MessageChannelMetrics) {
			configureChannelMetrics(name,
					(org.springframework.integration.support.management.MessageChannelMetrics) bean);
		}
		else if (bean instanceof org.springframework.integration.support.management.MessageHandlerMetrics) {
			configureHandlerMetrics(name,
					(org.springframework.integration.support.management.MessageHandlerMetrics) bean);
		}
		else if (bean instanceof org.springframework.integration.support.management.MessageSourceMetrics) {
			configureSourceMetrics(name,
					(org.springframework.integration.support.management.MessageSourceMetrics) bean);
			this.sourceConfigurers.values().forEach(c -> c
					.configure((org.springframework.integration.support.management.MessageSourceMetrics) bean, name));
		}
		return bean;
	}

	@SuppressWarnings("unchecked")
	private void configureChannelMetrics(String name,
			org.springframework.integration.support.management.MessageChannelMetrics bean) {

		org.springframework.integration.support.management.AbstractMessageChannelMetrics metrics;
		if (bean instanceof org.springframework.integration.support.management.PollableChannelManagement) {
			metrics = this.metricsFactory.createPollableChannelMetrics(name);
		}
		else {
			metrics = this.metricsFactory.createChannelMetrics(name);
		}
		Assert.state(metrics != null, "'metrics' must not be null");
		ManagementOverrides overrides = getOverrides(bean);
		Boolean enabled = PatternMatchUtils.smartMatch(name, this.enabledCountsPatterns);
		if (enabled != null) {
			bean.setCountsEnabled(enabled);
		}
		else {
			if (!overrides.countsConfigured) {
				bean.setCountsEnabled(this.defaultCountsEnabled);
			}
		}
		enabled = PatternMatchUtils.smartMatch(name, this.enabledStatsPatterns);
		if (enabled != null) {
			bean.setStatsEnabled(enabled);
			metrics.setFullStatsEnabled(enabled);
		}
		else {
			if (!overrides.statsConfigured) {
				bean.setStatsEnabled(this.defaultStatsEnabled);
				metrics.setFullStatsEnabled(this.defaultStatsEnabled);
			}
		}
		if (bean instanceof org.springframework.integration.support.management.ConfigurableMetricsAware
				&& !overrides.metricsConfigured) {
			((org.springframework.integration.support.management.ConfigurableMetricsAware<
					org.springframework.integration.support.management.AbstractMessageChannelMetrics>) bean)
						.configureMetrics(metrics);
		}
		this.channelsByName.put(name, bean);
	}

	@SuppressWarnings("unchecked")
	private void configureHandlerMetrics(String name,
			org.springframework.integration.support.management.MessageHandlerMetrics bean) {
		org.springframework.integration.support.management.AbstractMessageHandlerMetrics metrics
				= this.metricsFactory.createHandlerMetrics(name);
		Assert.state(metrics != null, "'metrics' must not be null");
		ManagementOverrides overrides = getOverrides(bean);
		Boolean enabled = PatternMatchUtils.smartMatch(name, this.enabledCountsPatterns);
		if (enabled != null) {
			bean.setCountsEnabled(enabled);
		}
		else {
			if (!overrides.countsConfigured) {
				bean.setCountsEnabled(this.defaultCountsEnabled);
			}
		}
		enabled = PatternMatchUtils.smartMatch(name, this.enabledStatsPatterns);
		if (enabled != null) {
			bean.setStatsEnabled(enabled);
			metrics.setFullStatsEnabled(enabled);
		}
		else {
			if (!overrides.statsConfigured) {
				bean.setStatsEnabled(this.defaultStatsEnabled);
				metrics.setFullStatsEnabled(this.defaultStatsEnabled);
			}
		}
		if (bean instanceof org.springframework.integration.support.management.ConfigurableMetricsAware
				&& !overrides.metricsConfigured) {
			((org.springframework.integration.support.management.ConfigurableMetricsAware<
					org.springframework.integration.support.management.AbstractMessageHandlerMetrics>) bean)
						.configureMetrics(metrics);
		}

		this.handlersByName.put(bean.getManagedName() != null ? bean.getManagedName() : name, bean);
	}

	private void configureSourceMetrics(String name,
			org.springframework.integration.support.management.MessageSourceMetrics bean) {

		Boolean enabled = PatternMatchUtils.smartMatch(name, this.enabledCountsPatterns);
		if (enabled != null) {
			bean.setCountsEnabled(enabled);
		}
		else {
			if (!getOverrides(bean).countsConfigured) {
				bean.setCountsEnabled(this.defaultCountsEnabled);
			}
		}
		this.sourcesByName.put(bean.getManagedName() != null ? bean.getManagedName() : name, bean);
	}

	private void registerComponentGauges() {
		this.metricsCaptor.gaugeBuilder("spring.integration.channels", this,
				(c) -> this.applicationContext.getBeansOfType(MessageChannel.class).size())
				.description("The number of message channels")
				.build();

		this.metricsCaptor.gaugeBuilder("spring.integration.handlers", this,
				(c) -> this.applicationContext.getBeansOfType(MessageHandler.class).size())
				.description("The number of message handlers")
				.build();

		this.metricsCaptor.gaugeBuilder("spring.integration.sources", this,
				(c) -> this.applicationContext.getBeansOfType(MessageSource.class).size())
				.description("The number of message sources")
				.build();
	}

	public String[] getChannelNames() {
		return this.channelsByName.keySet().toArray(new String[0]);
	}

	public String[] getHandlerNames() {
		return this.handlersByName.keySet().toArray(new String[0]);
	}

	public String[] getSourceNames() {
		return this.sourcesByName.keySet().toArray(new String[0]);
	}

	public org.springframework.integration.support.management.MessageChannelMetrics getChannelMetrics(String name) {
		if (this.channelsByName.containsKey(name)) {
			return this.channelsByName.get(name);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("No channel found for (" + name + ")");
		}
		return null;
	}

	public org.springframework.integration.support.management.MessageHandlerMetrics getHandlerMetrics(String name) {
		if (this.handlersByName.containsKey(name)) {
			return this.handlersByName.get(name);
		}
		if (this.handlersByName.containsKey(name + ".handler")) {
			return this.handlersByName.get(name + ".handler");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("No handler found for (" + name + ")");
		}
		return null;
	}

	public org.springframework.integration.support.management.MessageSourceMetrics getSourceMetrics(String name) {
		if (this.sourcesByName.containsKey(name)) {
			return this.sourcesByName.get(name);
		}
		if (this.sourcesByName.containsKey(name + ".source")) {
			return this.sourcesByName.get(name + ".source");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("No source found for (" + name + ")");
		}
		return null;
	}

	private static ManagementOverrides getOverrides(IntegrationManagement bean) {
		return bean.getOverrides() != null ? bean.getOverrides() : new ManagementOverrides();
	}

}

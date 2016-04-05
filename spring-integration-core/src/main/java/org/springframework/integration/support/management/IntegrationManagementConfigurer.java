/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.support.management;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;


/**
 * Configures beans that implement {@link IntegrationManagement}.
 * Configures counts, stats, logging for all (or selected) components.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class IntegrationManagementConfigurer implements SmartInitializingSingleton, ApplicationContextAware,
		BeanNameAware {

	public static final String MANAGEMENT_CONFIGURER_NAME = "integrationManagementConfigurer";

	private ApplicationContext applicationContext;

	private String beanName;

	private boolean defaultLoggingEnabled = true;

	private Boolean defaultCountsEnabled = false;

	private Boolean defaultStatsEnabled = false;

	private MetricsFactory metricsFactory;

	private String metricsFactoryBeanName;

	private String[] enabledCountsPatterns = {  };

	private String[] enabledStatsPatterns = {  };


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
	 * Defaults to {@link DefaultMetricsFactory}.
	 * @param metricsFactory the factory.
	 * @since 4.2
	 */
	public void setMetricsFactory(MetricsFactory metricsFactory) {
		this.metricsFactory = metricsFactory;
	}

	/**
	 * Set a metrics factory bean name.
	 * Is used if {@link #metricsFactory} isn't specified.
	 * @param metricsFactory the factory.
	 * @since 4.2
	 */
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
	 * @param enabledStatsPatterns the patterns.
	 */
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
	 */
	public void setDefaultStatsEnabled(Boolean defaultStatsEnabled) {
		this.defaultStatsEnabled = defaultStatsEnabled;
	}

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
	 * {@code logger.isDebuggingEnabled()} can be quite expensive and account for an inordinate amount of CPU
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
		if (this.metricsFactory == null && StringUtils.hasText(this.metricsFactoryBeanName)) {
			this.metricsFactory = this.applicationContext.getBean(this.metricsFactoryBeanName, MetricsFactory.class);
		}
		if (this.metricsFactory == null) {
			this.metricsFactory = new DefaultMetricsFactory();
		}
		Map<String, IntegrationManagement> managed = this.applicationContext.getBeansOfType(IntegrationManagement.class);
		for (Entry<String, IntegrationManagement> entry : managed.entrySet()) {
			IntegrationManagement bean = entry.getValue();
			bean.setLoggingEnabled(this.defaultLoggingEnabled);
			if (bean instanceof MessageChannelMetrics) {
				configureChannelMetrics(entry.getKey(), (MessageChannelMetrics) bean);
			}
			else if (bean instanceof MessageHandlerMetrics) {
				configureHandlerMetrics(entry.getKey(), (MessageHandlerMetrics) bean);
			}
			else if (bean instanceof MessageSourceMetrics) {
				configureSourceMetrics(entry.getKey(), (MessageSourceMetrics) bean);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void configureChannelMetrics(String name, MessageChannelMetrics bean) {
		AbstractMessageChannelMetrics metrics = this.metricsFactory.createChannelMetrics(name);
		Assert.state(metrics != null, "'metrics' must not be null");
		Boolean enabled = smartMatch(this.enabledCountsPatterns, name);
		if (enabled != null) {
			bean.setCountsEnabled(enabled);
		}
		else {
			bean.setCountsEnabled(this.defaultCountsEnabled);
		}
		enabled = smartMatch(this.enabledStatsPatterns, name);
		if (enabled != null) {
			bean.setStatsEnabled(enabled);
			metrics.setFullStatsEnabled(enabled);
		}
		else {
			bean.setStatsEnabled(this.defaultStatsEnabled);
			metrics.setFullStatsEnabled(this.defaultStatsEnabled);
		}
		if (bean instanceof ConfigurableMetricsAware) {
			((ConfigurableMetricsAware<AbstractMessageChannelMetrics>) bean).configureMetrics(metrics);
		}
	}

	@SuppressWarnings("unchecked")
	private void configureHandlerMetrics(String name, MessageHandlerMetrics bean) {
		AbstractMessageHandlerMetrics metrics = this.metricsFactory.createHandlerMetrics(name);
		Assert.state(metrics != null, "'metrics' must not be null");
		Boolean enabled = smartMatch(this.enabledCountsPatterns, name);
		if (enabled != null) {
			bean.setCountsEnabled(enabled);
		}
		else {
			bean.setCountsEnabled(this.defaultCountsEnabled);
		}
		enabled = smartMatch(this.enabledStatsPatterns, name);
		if (enabled != null) {
			bean.setStatsEnabled(enabled);
			metrics.setFullStatsEnabled(enabled);
		}
		else {
			bean.setStatsEnabled(this.defaultStatsEnabled);
			metrics.setFullStatsEnabled(this.defaultStatsEnabled);
		}
		if (bean instanceof ConfigurableMetricsAware) {
			((ConfigurableMetricsAware<AbstractMessageHandlerMetrics>) bean).configureMetrics(metrics);
		}
	}

	private void configureSourceMetrics(String name, MessageSourceMetrics bean) {
		Boolean enabled = smartMatch(this.enabledCountsPatterns, name);
		if (enabled != null) {
			bean.setCountsEnabled(enabled);
		}
		else {
			bean.setCountsEnabled(this.defaultCountsEnabled);
		}
	}

	/**
	 * Simple pattern match against the supplied patterns; also supports negated ('!')
	 * patterns. First match wins (positive or negative).
	 * @param patterns the patterns.
	 * @param name the name to match.
	 * @return null if no match; true for positive match; false for negative match.
	 */
	private Boolean smartMatch(String[] patterns, String name) {
		if (patterns != null) {
			for (String pattern : patterns) {
				boolean reverse = false;
				String patternToUse = pattern;
				if (pattern.startsWith("!")) {
					reverse = true;
					patternToUse = pattern.substring(1);
				}
				else if (pattern.startsWith("\\")) {
					patternToUse = pattern.substring(1);
				}
				if (PatternMatchUtils.simpleMatch(patternToUse, name)) {
					return !reverse;
				}
			}
		}
		return null; //NOSONAR - intentional null return
	}

	public MessageChannelMetrics getChannelMetrics(String name) {
		if (this.applicationContext.containsBean(name)) {
			return this.applicationContext.getBean(name, MessageChannelMetrics.class);
		}
		return null;
	}

	public MessageHandlerMetrics getHandlerMetrics(String name) {
		if (this.applicationContext.containsBean(name)) {
			return this.applicationContext.getBean(name, MessageHandlerMetrics.class);
		}
		return null;
	}

	public MessageSourceMetrics getSourceMetrics(String name) {
		if (this.applicationContext.containsBean(name)) {
			return this.applicationContext.getBean(name, MessageSourceMetrics.class);
		}
		return null;
	}

}

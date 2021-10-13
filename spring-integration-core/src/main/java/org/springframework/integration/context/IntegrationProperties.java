/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.context;

import java.util.Arrays;
import java.util.Properties;

import org.springframework.integration.util.JavaUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility class to encapsulate infrastructure Integration properties constants and their default values.
 * The default values can be overridden by the {@code META-INF/spring.integration.properties} with this entries
 * (includes their default values):
 * <ul>
 *   <li> {@code spring.integration.channels.autoCreate=true}
 *   <li> {@code spring.integration.channels.maxUnicastSubscribers=0x7fffffff}
 *   <li> {@code spring.integration.channels.maxBroadcastSubscribers=0x7fffffff}
 *   <li> {@code spring.integration.taskScheduler.poolSize=10}
 *   <li> {@code spring.integration.messagingTemplate.throwExceptionOnLateReply=false}
 *   <li> {@code spring.integration.readOnly.headers=}
 *   <li> {@code spring.integration.endpoints.noAutoStartup=}
 *   <li> {@code spring.integration.channels.error.requireSubscribers=true}
 *   <li> {@code spring.integration.channels.error.ignoreFailures=true}
 * </ul>
 *
 * @author Artem Bilan
 *
 * @since 3.0
 */
public final class IntegrationProperties {

	public static final String INTEGRATION_PROPERTIES_PREFIX = "spring.integration.";

	/**
	 * Specifies whether to allow create automatically {@link org.springframework.integration.channel.DirectChannel}
	 * beans for non-declared channels or not.
	 */
	public static final String CHANNELS_AUTOCREATE = INTEGRATION_PROPERTIES_PREFIX + "channels.autoCreate";

	/**
	 * Specifies the value for {@link org.springframework.integration.dispatcher.UnicastingDispatcher#maxSubscribers}
	 * in case of point-to-point channels (e.g. {@link org.springframework.integration.channel.ExecutorChannel}),
	 * if the attribute {@code max-subscribers} isn't configured on the channel component.
	 */
	public static final String CHANNELS_MAX_UNICAST_SUBSCRIBERS =
			INTEGRATION_PROPERTIES_PREFIX + "channels.maxUnicastSubscribers";

	/**
	 * Specifies the value for {@link org.springframework.integration.dispatcher.BroadcastingDispatcher#maxSubscribers}
	 * in case of point-to-point channels (e.g. {@link org.springframework.integration.channel.PublishSubscribeChannel}),
	 * if the attribute {@code max-subscribers} isn't configured on the channel component.
	 */
	public static final String CHANNELS_MAX_BROADCAST_SUBSCRIBERS =
			INTEGRATION_PROPERTIES_PREFIX + "channels.maxBroadcastSubscribers";


	/**
	 * Specifies the value for {@link org.springframework.integration.channel.PublishSubscribeChannel#requireSubscribers}
	 * on a global default {@link IntegrationContextUtils#ERROR_CHANNEL_BEAN_NAME}.
	 */
	public static final String ERROR_CHANNEL_REQUIRE_SUBSCRIBERS =
			INTEGRATION_PROPERTIES_PREFIX + "channels.error.requireSubscribers";

	/**
	 * Specifies the value for {@link org.springframework.integration.channel.PublishSubscribeChannel#ignoreFailures}
	 * on a global default {@link IntegrationContextUtils#ERROR_CHANNEL_BEAN_NAME}.
	 */
	public static final String ERROR_CHANNEL_IGNORE_FAILURES =
			INTEGRATION_PROPERTIES_PREFIX + "channels.error.ignoreFailures";


	/**
	 * Specifies the value of {@link org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler#poolSize}
	 * for the {@code taskScheduler} bean initialized by the Integration infrastructure.
	 */
	public static final String TASK_SCHEDULER_POOL_SIZE = INTEGRATION_PROPERTIES_PREFIX + "taskScheduler.poolSize";

	/**
	 * Specifies the value of {@link org.springframework.messaging.core.GenericMessagingTemplate#throwExceptionOnLateReply}.
	 */
	public static final String THROW_EXCEPTION_ON_LATE_REPLY =
			INTEGRATION_PROPERTIES_PREFIX + "messagingTemplate.throwExceptionOnLateReply";

	/**
	 * Specifies the value of {@link org.springframework.integration.support.DefaultMessageBuilderFactory#readOnlyHeaders}.
	 */
	public static final String READ_ONLY_HEADERS = INTEGRATION_PROPERTIES_PREFIX + "readOnly.headers";

	/**
	 * Specifies the value of {@link org.springframework.integration.endpoint.AbstractEndpoint#autoStartup}.
	 */
	public static final String ENDPOINTS_NO_AUTO_STARTUP = INTEGRATION_PROPERTIES_PREFIX + "endpoints.noAutoStartup";

	private static final Properties DEFAULTS;

	private boolean channelsAutoCreate = true;

	private int channelsMaxUnicastSubscribers = Integer.MAX_VALUE;

	private int channelsMaxBroadcastSubscribers = Integer.MAX_VALUE;

	private boolean errorChannelRequireSubscribers = true;

	private boolean errorChannelIgnoreFailures = true;

	private int taskSchedulerPoolSize = 10; // NOSONAR

	private boolean messagingTemplateThrowExceptionOnLateReply = false;

	private String[] readOnlyHeaders = { };

	private String[] noAutoStartupEndpoints = { };

	static {
		DEFAULTS = new IntegrationProperties().toProperties();
	}

	/**
	 * Configure a value for {@link #CHANNELS_AUTOCREATE} option.
	 * @param channelsAutoCreate the value for {@link #CHANNELS_AUTOCREATE} option.
	 */
	public void setChannelsAutoCreate(boolean channelsAutoCreate) {
		this.channelsAutoCreate = channelsAutoCreate;
	}

	/**
	 * Return the value of {@link #CHANNELS_AUTOCREATE} option.
	 * @return the value of {@link #CHANNELS_AUTOCREATE} option.
	 */
	public boolean isChannelsAutoCreate() {
		return this.channelsAutoCreate;
	}

	/**
	 * Configure a value for {@link #CHANNELS_MAX_UNICAST_SUBSCRIBERS} option.
	 * @param channelsMaxUnicastSubscribers the value for {@link #CHANNELS_MAX_UNICAST_SUBSCRIBERS} option.
	 */
	public void setChannelsMaxUnicastSubscribers(int channelsMaxUnicastSubscribers) {
		this.channelsMaxUnicastSubscribers = channelsMaxUnicastSubscribers;
	}

	/**
	 * Return the value of {@link #CHANNELS_MAX_UNICAST_SUBSCRIBERS} option.
	 * @return the value of {@link #CHANNELS_MAX_UNICAST_SUBSCRIBERS} option.
	 */
	public int getChannelsMaxUnicastSubscribers() {
		return this.channelsMaxUnicastSubscribers;
	}

	/**
	 * Configure a value for {@link #CHANNELS_MAX_BROADCAST_SUBSCRIBERS} option.
	 * @param channelsMaxBroadcastSubscribers the value for {@link #CHANNELS_MAX_BROADCAST_SUBSCRIBERS} option.
	 */
	public void setChannelsMaxBroadcastSubscribers(int channelsMaxBroadcastSubscribers) {
		this.channelsMaxBroadcastSubscribers = channelsMaxBroadcastSubscribers;
	}

	/**
	 * Return the value of {@link #CHANNELS_MAX_BROADCAST_SUBSCRIBERS} option.
	 * @return the value of {@link #CHANNELS_MAX_BROADCAST_SUBSCRIBERS} option.
	 */
	public int getChannelsMaxBroadcastSubscribers() {
		return this.channelsMaxBroadcastSubscribers;
	}

	/**
	 * Configure a value for {@link #ERROR_CHANNEL_REQUIRE_SUBSCRIBERS} option.
	 * @param errorChannelRequireSubscribers the value for {@link #ERROR_CHANNEL_REQUIRE_SUBSCRIBERS} option.
	 */
	public void setErrorChannelRequireSubscribers(boolean errorChannelRequireSubscribers) {
		this.errorChannelRequireSubscribers = errorChannelRequireSubscribers;
	}

	/**
	 * Return the value of {@link #ERROR_CHANNEL_REQUIRE_SUBSCRIBERS} option.
	 * @return the value of {@link #ERROR_CHANNEL_REQUIRE_SUBSCRIBERS} option.
	 */
	public boolean isErrorChannelRequireSubscribers() {
		return this.errorChannelRequireSubscribers;
	}

	/**
	 * Configure a value for {@link #ERROR_CHANNEL_IGNORE_FAILURES} option.
	 * @param errorChannelIgnoreFailures the value for {@link #ERROR_CHANNEL_IGNORE_FAILURES} option.
	 */
	public void setErrorChannelIgnoreFailures(boolean errorChannelIgnoreFailures) {
		this.errorChannelIgnoreFailures = errorChannelIgnoreFailures;
	}

	/**
	 * Return the value of {@link #ERROR_CHANNEL_IGNORE_FAILURES} option.
	 * @return the value of {@link #ERROR_CHANNEL_IGNORE_FAILURES} option.
	 */
	public boolean isErrorChannelIgnoreFailures() {
		return this.errorChannelIgnoreFailures;
	}

	/**
	 * Configure a value for {@link #TASK_SCHEDULER_POOL_SIZE} option.
	 * @param taskSchedulerPoolSize the value for {@link #TASK_SCHEDULER_POOL_SIZE} option.
	 */
	public void setTaskSchedulerPoolSize(int taskSchedulerPoolSize) {
		this.taskSchedulerPoolSize = taskSchedulerPoolSize;
	}

	/**
	 * Return the value of {@link #TASK_SCHEDULER_POOL_SIZE} option.
	 * @return the value of {@link #TASK_SCHEDULER_POOL_SIZE} option.
	 */
	public int getTaskSchedulerPoolSize() {
		return this.taskSchedulerPoolSize;
	}

	/**
	 * Configure a value for {@link #THROW_EXCEPTION_ON_LATE_REPLY} option.
	 * @param messagingTemplateThrowExceptionOnLateReply the value for {@link #THROW_EXCEPTION_ON_LATE_REPLY} option.
	 */
	public void setMessagingTemplateThrowExceptionOnLateReply(boolean messagingTemplateThrowExceptionOnLateReply) {
		this.messagingTemplateThrowExceptionOnLateReply = messagingTemplateThrowExceptionOnLateReply;
	}

	/**
	 * Return the value of {@link #THROW_EXCEPTION_ON_LATE_REPLY} option.
	 * @return the value of {@link #THROW_EXCEPTION_ON_LATE_REPLY} option.
	 */
	public boolean isMessagingTemplateThrowExceptionOnLateReply() {
		return this.messagingTemplateThrowExceptionOnLateReply;
	}

	/**
	 * Configure a value for {@link #READ_ONLY_HEADERS} option.
	 * @param readOnlyHeaders the value for {@link #READ_ONLY_HEADERS} option.
	 */
	public void setReadOnlyHeaders(String... readOnlyHeaders) {
		Assert.notNull(readOnlyHeaders, "'readOnlyHeaders' must not be null.");
		this.readOnlyHeaders = Arrays.copyOf(readOnlyHeaders, readOnlyHeaders.length);
	}

	/**
	 * Return the value of {@link #READ_ONLY_HEADERS} option.
	 * @return the value of {@link #READ_ONLY_HEADERS} option.
	 */
	public String[] getReadOnlyHeaders() {
		return Arrays.copyOf(this.readOnlyHeaders, this.readOnlyHeaders.length);
	}

	/**
	 * Configure a value for {@link #ENDPOINTS_NO_AUTO_STARTUP} option.
	 * @param noAutoStartupEndpoints the value for {@link #ENDPOINTS_NO_AUTO_STARTUP} option.
	 */
	public void setNoAutoStartupEndpoints(String... noAutoStartupEndpoints) {
		Assert.notNull(noAutoStartupEndpoints, "'noAutoStartupEndpoints' must not be null.");
		this.noAutoStartupEndpoints = Arrays.copyOf(noAutoStartupEndpoints, noAutoStartupEndpoints.length);
	}

	/**
	 * Return the value of {@link #ENDPOINTS_NO_AUTO_STARTUP} option.
	 * @return the value of {@link #ENDPOINTS_NO_AUTO_STARTUP} option.
	 */
	public String[] getNoAutoStartupEndpoints() {
		return Arrays.copyOf(this.noAutoStartupEndpoints, this.noAutoStartupEndpoints.length);
	}

	/**
	 * Represent the current instance as a {@link Properties}.
	 * @return the {@link Properties} representation.
	 * @since 5.5
	 */
	public Properties toProperties() {
		Properties properties = new Properties();

		properties.setProperty(CHANNELS_AUTOCREATE, "" + this.channelsAutoCreate);
		properties.setProperty(CHANNELS_MAX_UNICAST_SUBSCRIBERS, "" + this.channelsMaxUnicastSubscribers);
		properties.setProperty(CHANNELS_MAX_BROADCAST_SUBSCRIBERS, "" + this.channelsMaxBroadcastSubscribers);
		properties.setProperty(ERROR_CHANNEL_REQUIRE_SUBSCRIBERS, "" + this.errorChannelRequireSubscribers);
		properties.setProperty(ERROR_CHANNEL_IGNORE_FAILURES, "" + this.errorChannelIgnoreFailures);
		properties.setProperty(TASK_SCHEDULER_POOL_SIZE, "" + this.taskSchedulerPoolSize);
		properties.setProperty(THROW_EXCEPTION_ON_LATE_REPLY, "" + this.messagingTemplateThrowExceptionOnLateReply);
		properties.setProperty(READ_ONLY_HEADERS, StringUtils.arrayToCommaDelimitedString(this.readOnlyHeaders));
		properties.setProperty(ENDPOINTS_NO_AUTO_STARTUP,
				StringUtils.arrayToCommaDelimitedString(this.noAutoStartupEndpoints));

		return properties;
	}

	/**
	 * Parse a provided {@link Properties} and build an {@link IntegrationProperties} instance.
	 * @param properties the {@link Properties} to parse entries for {@link IntegrationProperties}.
	 * @return {@link IntegrationProperties} based on the provided {@link Properties}.
	 * @since 5.5
	 */
	public static IntegrationProperties parse(Properties properties) {
		IntegrationProperties integrationProperties = new IntegrationProperties();
		JavaUtils.INSTANCE
				.acceptIfHasText(properties.getProperty(CHANNELS_AUTOCREATE),
						(value) -> integrationProperties.setChannelsAutoCreate(Boolean.parseBoolean(value)))
				.acceptIfHasText(properties.getProperty(CHANNELS_MAX_UNICAST_SUBSCRIBERS),
						(value) -> integrationProperties.setChannelsMaxUnicastSubscribers(Integer.parseInt(value)))
				.acceptIfHasText(properties.getProperty(CHANNELS_MAX_BROADCAST_SUBSCRIBERS),
						(value) -> integrationProperties.setChannelsMaxBroadcastSubscribers(Integer.parseInt(value)))
				.acceptIfHasText(properties.getProperty(ERROR_CHANNEL_REQUIRE_SUBSCRIBERS),
						(value) -> integrationProperties.setErrorChannelRequireSubscribers(Boolean.parseBoolean(value)))
				.acceptIfHasText(properties.getProperty(ERROR_CHANNEL_IGNORE_FAILURES),
						(value) -> integrationProperties.setErrorChannelIgnoreFailures(Boolean.parseBoolean(value)))
				.acceptIfHasText(properties.getProperty(TASK_SCHEDULER_POOL_SIZE),
						(value) -> integrationProperties.setTaskSchedulerPoolSize(Integer.parseInt(value)))
				.acceptIfHasText(properties.getProperty(THROW_EXCEPTION_ON_LATE_REPLY),
						(value) -> integrationProperties.setMessagingTemplateThrowExceptionOnLateReply(
								Boolean.parseBoolean(value)))
				.acceptIfHasText(properties.getProperty(READ_ONLY_HEADERS),
						(value) -> integrationProperties.setReadOnlyHeaders(
								StringUtils.commaDelimitedListToStringArray(value)))
				.acceptIfHasText(properties.getProperty(ENDPOINTS_NO_AUTO_STARTUP),
						(value) -> integrationProperties.setNoAutoStartupEndpoints(
								StringUtils.commaDelimitedListToStringArray(value)));
		return integrationProperties;
	}

	/**
	 * @return {@link Properties} with default values for Integration properties.
	 */
	public static Properties defaults() {
		return DEFAULTS;
	}

	/**
	 * Build the bean property definition expression to resolve the value
	 * from Integration properties within the bean building phase.
	 * @param key the Integration property key.
	 * @return the bean property definition expression.
	 * @throws IllegalArgumentException if provided {@code key} isn't an Integration property.
	 */
	public static String getExpressionFor(String key) {
		if (DEFAULTS.containsKey(key)) {
			return "#{T(org.springframework.integration.context.IntegrationContextUtils)" +
					".getIntegrationProperties(beanFactory).getProperty('" + key + "')}";
		}
		else {
			throw new IllegalArgumentException("The provided key [" + key +
					"] isn't the one of Integration properties: " + DEFAULTS.keySet());
		}
	}

}

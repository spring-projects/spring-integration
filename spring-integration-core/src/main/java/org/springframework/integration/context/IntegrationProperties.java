/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.context;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Utility class to encapsulate infrastructure Integration properties constants and
 * their default values from resources 'META-INF/spring.integration.default.properties'.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public final class IntegrationProperties {

	/**
	 * Specifies whether to allow create automatically {@link org.springframework.integration.channel.DirectChannel}
	 * beans for non-declared channels or not.
	 */
	public static final String CHANNELS_AUTOCREATE = "channels.autoCreate";

	/**
	 * Specifies the value for {@link org.springframework.integration.dispatcher.UnicastingDispatcher#maxSubscribers}
	 * in case of point-to-point channels (e.g. {@link org.springframework.integration.channel.ExecutorChannel}),
	 * if the attribute {@code max-subscribers} isn't configured on the channel component.
	 */
	public static final String CHANNELS_MAX_UNICAST_SUBSCRIBERS = "channels.maxUnicastSubscribers";

	/**
	 * Specifies the value for {@link org.springframework.integration.dispatcher.BroadcastingDispatcher#maxSubscribers}
	 * in case of point-to-point channels (e.g. {@link org.springframework.integration.channel.PublishSubscribeChannel}),
	 * if the attribute {@code max-subscribers} isn't configured on the channel component.
	 */
	public static final String CHANNELS_MAX_BROADCAST_SUBSCRIBERS = "channels.maxBroadcastSubscribers";


//	TODO public static final String LATE_REPLY_LOGGING_LEVEL = "messagingTemplate.lateReply.logging.level";

//	TODO public static final String TASKSCHEDULER_POOLSIZE = "taskScheduler.poolSize";

	private static Properties defaults;

	static {
		String resourcePattern = "classpath*:META-INF/spring.integration.default.properties";
		try {
			ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(IntegrationProperties.class.getClassLoader());
			Resource[] defaultResources = resourceResolver.getResources(resourcePattern);

			PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
			propertiesFactoryBean.setLocations(defaultResources);
			propertiesFactoryBean.afterPropertiesSet();
			defaults = propertiesFactoryBean.getObject();
		}
		catch (IOException e) {
			throw new IllegalStateException("Can't load '" + resourcePattern + "' resources.", e);
		}
	}

	/**
	 * @return {@link Properties} with default values for Integration properties
	 *         from resources 'META-INF/spring.integration.default.properties'.
	 */
	public static Properties defaults() {
		return defaults;
	}

	/**
	 * Build the bean property definition expression to resolve the value
	 * from Integration properties within the bean building phase.
	 *
	 * @param key the Integration property key.
	 * @return the bean property definition expression.
	 * @throws IllegalArgumentException if provided {@code key} isn't an Integration property.
	 */
	public static String getExpressionFor(String key) {
		if (defaults.containsKey(key)) {
			return "#{T(" + IntegrationContextUtils.class.getName() + ").getIntegrationProperties(beanFactory).getProperty('" + key + "')}";
		}
		else {
			throw new IllegalArgumentException("The provided key [" + key + "] isn't the one of Integration properties: " + defaults.keySet());
		}
	}

	private IntegrationProperties() {
	}

}

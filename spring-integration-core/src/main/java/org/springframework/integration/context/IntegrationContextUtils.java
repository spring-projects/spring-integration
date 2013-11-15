/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Properties;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Utility methods for accessing common integration components from the BeanFactory.
 *
 * @author Mark Fisher
 * @author Josh Long
 * @author Artem Bilan
 */
public abstract class IntegrationContextUtils {

	public static final String TASK_SCHEDULER_BEAN_NAME = "taskScheduler";

	public static final String ERROR_CHANNEL_BEAN_NAME = "errorChannel";

	public static final String NULL_CHANNEL_BEAN_NAME = "nullChannel";

	public static final String METADATA_STORE_BEAN_NAME = "metadataStore";

	public static final String INTEGRATION_CONVERSION_SERVICE_BEAN_NAME = "integrationConversionService";

	public static final String INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME = "integrationEvaluationContext";

	public static final String INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME = "integrationHeaderChannelRegistry";

	public static final String INTEGRATION_PROPERTIES_BEAN_NAME = "integrationProperties";

	private static final Properties EMPTY_PROPERTIES = new Properties();

	/**
	 * Return the {@link MetadataStore} bean whose name is "metadataStore".
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 */
	public static MetadataStore getMetadataStore(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, METADATA_STORE_BEAN_NAME, MetadataStore.class);
	}

	/**
	 * Return the {@link MessageChannel} bean whose name is "errorChannel".
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 */
	public static MessageChannel getErrorChannel(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, ERROR_CHANNEL_BEAN_NAME, MessageChannel.class);
	}

	/**
	 * Return the {@link TaskScheduler} bean whose name is "taskScheduler" if available.
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 */
	public static TaskScheduler getTaskScheduler(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class);
	}

	/**
	 * Return the {@link TaskScheduler} bean whose name is "taskScheduler".
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @throws IllegalStateException if no such bean is available
	 */
	public static TaskScheduler getRequiredTaskScheduler(BeanFactory beanFactory) {
		TaskScheduler taskScheduler = getTaskScheduler(beanFactory);
		Assert.state(taskScheduler != null, "No such bean '" + TASK_SCHEDULER_BEAN_NAME + "'");
		return taskScheduler;
	}

	/**
	 * Return the {@link ConversionService} bean whose name is "integrationConversionService" if available.
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 */
	public static ConversionService getConversionService(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, INTEGRATION_CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
	}

	/**
	 * Return the instance of {@link StandardEvaluationContext} bean whose name is "integrationEvaluationContext" .
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 */
	public static StandardEvaluationContext getEvaluationContext(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME, StandardEvaluationContext.class);
	}

	private static <T> T getBeanOfType(BeanFactory beanFactory, String beanName, Class<T> type) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		if (!beanFactory.containsBean(beanName)) {
			return null;
		}
		return beanFactory.getBean(beanName, type);
	}

	/**
	 * @return the global {@link IntegrationContextUtils#INTEGRATION_PROPERTIES_BEAN_NAME}
	 *         bean from provided {@code #beanFactory}, which represents the merged
	 *         properties values from all 'META-INF/spring.integration.default.properties'
	 *         and 'META-INF/spring.integration.properties'.
	 *         May return {@link IntegrationContextUtils#EMPTY_PROPERTIES} if there is no
	 *         {@link IntegrationContextUtils#INTEGRATION_PROPERTIES_BEAN_NAME} bean within
	 *         provided {@code #beanFactory} or provided {@code #beanFactory} is null.
	 */
	public static Properties getIntegrationProperties(BeanFactory beanFactory) {
		Properties properties = null;
		if (beanFactory != null) {
			properties = getBeanOfType(beanFactory, INTEGRATION_PROPERTIES_BEAN_NAME, Properties.class);
		}
		if (properties == null) {
			properties = EMPTY_PROPERTIES;
		}
		return properties;
	}

}

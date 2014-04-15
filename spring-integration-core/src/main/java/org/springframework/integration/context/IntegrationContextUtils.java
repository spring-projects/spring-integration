/*
 * Copyright 2002-2014 the original author or authors.
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
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.config.IntegrationConfigUtils;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Utility methods for accessing common integration components from the BeanFactory.
 *
 * @author Mark Fisher
 * @author Josh Long
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class IntegrationContextUtils {

	public static final String TASK_SCHEDULER_BEAN_NAME = "taskScheduler";

	public static final String ERROR_CHANNEL_BEAN_NAME = "errorChannel";

	public static final String NULL_CHANNEL_BEAN_NAME = "nullChannel";

	public static final String METADATA_STORE_BEAN_NAME = "metadataStore";

	public static final String CONVERTER_REGISTRAR_BEAN_NAME = "converterRegistrar";

	public static final String INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME = "integrationEvaluationContext";

	public static final String INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME = "integrationHeaderChannelRegistry";

	public static final String INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME = "integrationGlobalProperties";

	public static final String CHANNEL_INITIALIZER_BEAN_NAME = "channelInitializer";

	public static final String AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME = "$autoCreateChannelCandidates";

	public static final String DEFAULT_CONFIGURING_POSTPROCESSOR_BEAN_NAME = "DefaultConfiguringBeanFactoryPostProcessor";

	public static final String MESSAGING_ANNOTATION_POSTPROCESSOR_NAME = IntegrationConfigUtils.BASE_PACKAGE
			+ ".internalMessagingAnnotationPostProcessor";

	public static final String PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME = IntegrationConfigUtils.BASE_PACKAGE +
			".internalPublisherAnnotationBeanPostProcessor";

	public static final String INTEGRATION_CONFIGURATION_POST_PROCESSOR_BEAN_NAME = "IntegrationConfigurationBeanFactoryPostProcessor";

	public static final String INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME = "messageHistoryConfigurer";

	public static final String INTEGRATION_DATATYPE_CHANNEL_MESSAGE_CONVERTER_BEAN_NAME = "datatypeChannelMessageConverter";

	public static final String INTEGRATION_FIXED_SUBSCRIBER_CHANNEL_BPP_BEAN_NAME = "fixedSubscriberChannelBeanFactoryPostProcessor";

	public static final String GLOBAL_CHANNEL_INTERCEPTOR_PROCESSOR_BEAN_NAME = "globalChannelInterceptorProcessor";

	/**
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return The {@link MetadataStore} bean whose name is "metadataStore".
	 */
	public static MetadataStore getMetadataStore(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, METADATA_STORE_BEAN_NAME, MetadataStore.class);
	}

	/**
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return The {@link MessageChannel} bean whose name is "errorChannel".
	 */
	public static MessageChannel getErrorChannel(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, ERROR_CHANNEL_BEAN_NAME, MessageChannel.class);
	}

	/**
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return The {@link TaskScheduler} bean whose name is "taskScheduler" if available.
	 */
	public static TaskScheduler getTaskScheduler(BeanFactory beanFactory) {
		return getBeanOfType(beanFactory, TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class);
	}

	/**
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return The {@link TaskScheduler} bean whose name is "taskScheduler".
	 * @throws IllegalStateException if no such bean is available
	 */
	public static TaskScheduler getRequiredTaskScheduler(BeanFactory beanFactory) {
		TaskScheduler taskScheduler = getTaskScheduler(beanFactory);
		Assert.state(taskScheduler != null, "No such bean '" + TASK_SCHEDULER_BEAN_NAME + "'");
		return taskScheduler;
	}

	/**
	 * @param beanFactory BeanFactory for lookup, must not be null.
	 * @return the instance of {@link StandardEvaluationContext} bean whose name is "integrationEvaluationContext" .
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
	 * @param beanFactory The bean factory.
	 * @return the global {@link IntegrationContextUtils#INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME}
	 *         bean from provided {@code #beanFactory}, which represents the merged
	 *         properties values from all 'META-INF/spring.integration.default.properties'
	 *         and 'META-INF/spring.integration.properties'.
	 *         Or user-defined {@link Properties} bean.
	 *         May return only {@link IntegrationProperties#defaults()} if there is no
	 *         {@link IntegrationContextUtils#INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME} bean within
	 *         provided {@code #beanFactory} or provided {@code #beanFactory} is null.
	 */
	public static Properties getIntegrationProperties(BeanFactory beanFactory) {
		Properties properties = new Properties();
		properties.putAll(IntegrationProperties.defaults());
		if (beanFactory != null) {
			Properties userProperties = getBeanOfType(beanFactory, INTEGRATION_GLOBAL_PROPERTIES_BEAN_NAME, Properties.class);
			if (userProperties != null) {
				properties.putAll(userProperties);
			}
		}
		return properties;
	}

}

/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;

import org.springframework.core.convert.ConversionService;

import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.metadata.MetadataPersister;
import org.springframework.integration.scheduling.PollerMetadata;

import org.springframework.scheduling.TaskScheduler;

import org.springframework.util.Assert;


/**
 * Utility methods for accessing common integration components from the BeanFactory.
 *
 * @author Mark Fisher
 * @author Josh Long 
 */
public abstract class IntegrationContextUtils {

    public static final String METADATA_PERSISTER_BEAN_NAME = "metadataPersister";

    public static final String TASK_SCHEDULER_BEAN_NAME = "taskScheduler";

    public static final String ERROR_CHANNEL_BEAN_NAME = "errorChannel";

    public static final String NULL_CHANNEL_BEAN_NAME = "nullChannel";

    public static final String INTEGRATION_CONVERSION_SERVICE_BEAN_NAME = "integrationConversionService";

    public static final String DEFAULT_POLLER_METADATA_BEAN_NAME = "org.springframework.integration.context.defaultPollerMetadata";

    public static MetadataPersister getMetadataPersister(BeanFactory beanFactory) {
        return getBeanOfType(beanFactory, METADATA_PERSISTER_BEAN_NAME, MetadataPersister.class);
    }

    public static MessageChannel getErrorChannel(BeanFactory beanFactory) {
        return getBeanOfType(beanFactory, ERROR_CHANNEL_BEAN_NAME, MessageChannel.class);
    }

    public static TaskScheduler getTaskScheduler(BeanFactory beanFactory) {
        return getBeanOfType(beanFactory, TASK_SCHEDULER_BEAN_NAME, TaskScheduler.class);
    }

    public static TaskScheduler getRequiredTaskScheduler(BeanFactory beanFactory) {
        TaskScheduler taskScheduler = getTaskScheduler(beanFactory);
        Assert.state(taskScheduler != null, "No such bean '" + TASK_SCHEDULER_BEAN_NAME + "'");
        return taskScheduler;
    }

    public static PollerMetadata getDefaultPollerMetadata(BeanFactory beanFactory) {
        return getBeanOfType(beanFactory, DEFAULT_POLLER_METADATA_BEAN_NAME, PollerMetadata.class);
    }

    public static ConversionService getConversionService(BeanFactory beanFactory) {
        return getBeanOfType(beanFactory, INTEGRATION_CONVERSION_SERVICE_BEAN_NAME, ConversionService.class);
    }

    private static <T> T getBeanOfType(BeanFactory beanFactory, String beanName, Class<T> type) {
        if (!beanFactory.containsBean(beanName)) {
            return null;
        }
        return beanFactory.getBean(beanName, type);
    }
}

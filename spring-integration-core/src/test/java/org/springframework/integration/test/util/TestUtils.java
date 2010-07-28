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

package org.springframework.integration.test.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.hamcrest.Matcher;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.context.BeanFactoryChannelResolver;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageDeliveryException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.integration.history.NamedComponent;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public abstract class TestUtils {

    public static Object getPropertyValue(Object root, String propertyPath) {
        Object value = null;
        DirectFieldAccessor accessor = new DirectFieldAccessor(root);
        String[] tokens = propertyPath.split("\\.");
        for (int i = 0; i < tokens.length; i++) {
            value = accessor.getPropertyValue(tokens[i]);
            if (value != null) {
                accessor = new DirectFieldAccessor(value);
            } else if (i == tokens.length - 1) {
                return null;
            } else {
                throw new IllegalArgumentException(
                        "intermediate property '" + tokens[i] + "' is null");
            }
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getPropertyValue(Object root, String propertyPath, Class<T> type) {
        Object value = getPropertyValue(root, propertyPath);
        Assert.isAssignable(type, value.getClass());
        return (T) value;
    }

    public static TestApplicationContext createTestApplicationContext() {
        TestApplicationContext context = new TestApplicationContext();
        ErrorHandler errorHandler = new MessagePublishingErrorHandler(new BeanFactoryChannelResolver(context));
        ThreadPoolTaskScheduler scheduler = createTaskScheduler(10);
        scheduler.setErrorHandler(errorHandler);
        registerBean(IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, scheduler, context);
        return context;
    }

    public static ThreadPoolTaskScheduler createTaskScheduler(int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setRejectedExecutionHandler(new CallerRunsPolicy());
        scheduler.afterPropertiesSet();
        return scheduler;
    }

    private static void registerBean(String beanName, Object bean, BeanFactory beanFactory) {
        Assert.notNull(beanName, "bean name must not be null");
        ConfigurableListableBeanFactory configurableListableBeanFactory = null;
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            configurableListableBeanFactory = (ConfigurableListableBeanFactory) beanFactory;
        } else if (beanFactory instanceof GenericApplicationContext) {
            configurableListableBeanFactory = ((GenericApplicationContext) beanFactory).getBeanFactory();
        }
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }
        if (bean instanceof BeanFactoryAware) {
            ((BeanFactoryAware) bean).setBeanFactory(beanFactory);
        }
        if (bean instanceof InitializingBean) {
            try {
                ((InitializingBean) bean).afterPropertiesSet();
            }
            catch (Exception e) {
                throw new FatalBeanException("failed to register bean with test context", e);
            }
        }
        configurableListableBeanFactory.registerSingleton(beanName, bean);
    }


    public static class TestApplicationContext extends GenericApplicationContext {

        private TestApplicationContext() {
            super();
        }

        public void registerChannel(String channelName, MessageChannel channel) {
            if (channel instanceof NamedComponent && ((NamedComponent) channel).getComponentName() != null) {
                if (channelName == null) {
                    channelName = ((NamedComponent) channel).getComponentName();
                }
                else {
                    Assert.isTrue(((NamedComponent) channel).getComponentName().equals(channelName),
                            "channel name has already been set with a conflicting value");
                }
            }
            registerBean(channelName, channel, this);
        }

        public void registerEndpoint(String endpointName, AbstractEndpoint endpoint) {
            if (endpoint instanceof AbstractPollingEndpoint) {
                DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
                if (accessor.getPropertyValue("trigger") == null) {
                    ((AbstractPollingEndpoint) endpoint).setTrigger(new PeriodicTrigger(10));
                }
            }
            registerBean(endpointName, endpoint, this);
        }
    }

    @SuppressWarnings("unchecked")
    public static MessageHandler handlerExpecting(final Matcher<Message> messageMatcher) {
        return new MessageHandler() {
            public void handleMessage(Message<?> message) throws MessageRejectedException, MessageHandlingException, MessageDeliveryException {
                assertThat(message, is(messageMatcher));
            }
        };
    }
}
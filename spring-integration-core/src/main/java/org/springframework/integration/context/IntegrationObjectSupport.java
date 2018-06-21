/*
 * Copyright 2002-2018 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A base class that provides convenient access to the bean factory as
 * well as {@link TaskScheduler} and {@link ConversionService} instances.
 *
 * <p>This is intended to be used as a base class for internal framework
 * components whereas code built upon the integration framework should not
 * require tight coupling with the context but rather rely on standard
 * dependency injection.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Josh Long
 * @author Stefan Ferstl
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class IntegrationObjectSupport implements BeanNameAware, NamedComponent,
		ApplicationContextAware, BeanFactoryAware, InitializingBean, ExpressionCapable {

	protected static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	/**
	 * Logger that is available to subclasses
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	private final ConversionService defaultConversionService = DefaultConversionService.getSharedInstance();

	private DestinationResolver<MessageChannel> channelResolver;

	private String beanName;

	private String componentName;

	private BeanFactory beanFactory;

	private TaskScheduler taskScheduler;

	private Properties integrationProperties = IntegrationProperties.defaults();

	private ConversionService conversionService;

	private ApplicationContext applicationContext;

	private MessageBuilderFactory messageBuilderFactory;

	private Expression expression;

	private boolean initialized;

	@Override
	public final void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * Will return the name of this component identified by {@link #componentName} field.
	 * If {@link #componentName} was not set this method will default to the 'beanName' of this component;
	 */
	@Override
	public String getComponentName() {
		return StringUtils.hasText(this.componentName) ? this.componentName : this.beanName;
	}

	/**
	 * Sets the name of this component.
	 * @param componentName The component name.
	 */
	public void setComponentName(String componentName) {
		this.componentName = componentName;
	}

	/**
	 * Subclasses may implement this method to provide component type information.
	 */
	@Override
	public String getComponentType() {
		return null;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		this.beanFactory = beanFactory;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		this.applicationContext = applicationContext;
	}

	/**
	 * Specify the {@link DestinationResolver} strategy to use.
	 * The default is a BeanFactoryChannelResolver.
	 * @param channelResolver The channel resolver.
	 */
	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.channelResolver = channelResolver;
	}

	@Override
	public Expression getExpression() {
		return this.expression;
	}

	/**
	 * For expression-based components, set the primary expression.
	 * @param expression the expression.
	 * @since 4.3
	 */
	public final void setPrimaryExpression(Expression expression) {
		this.expression = expression;
	}

	@Override
	public final void afterPropertiesSet() {
		this.integrationProperties = IntegrationContextUtils.getIntegrationProperties(this.beanFactory);
		try {
			if (this.messageBuilderFactory == null) {
				if (this.beanFactory != null) {
					this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
				}
				else {
					this.messageBuilderFactory = new DefaultMessageBuilderFactory();
				}
			}
			this.onInit();
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new BeanInitializationException("failed to initialize", e);
		}

		this.initialized = true;
	}

	/**
	 * Subclasses may implement this for initialization logic.
	 * @throws Exception Any exception.
	 */
	protected void onInit() throws Exception {
	}

	/**
	 * Return the status of this component if it has been initialized already.
	 * @return the flag if this component has been initialized already.
	 */
	protected boolean isInitialized() {
		return this.initialized;
	}

	protected BeanFactory getBeanFactory() {
		return this.beanFactory;
	}

	protected TaskScheduler getTaskScheduler() {
		if (this.taskScheduler == null && this.beanFactory != null) {
			this.taskScheduler = IntegrationContextUtils.getTaskScheduler(this.beanFactory);
		}
		return this.taskScheduler;
	}

	protected DestinationResolver<MessageChannel> getChannelResolver() {
		if (this.channelResolver == null) {
			this.channelResolver = new BeanFactoryChannelResolver(this.beanFactory);
		}
		return this.channelResolver;
	}

	protected void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "taskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	public ConversionService getConversionService() {
		if (this.conversionService == null && this.beanFactory != null) {
			synchronized (this) {
				if (this.conversionService == null) {
					this.conversionService = IntegrationUtils.getConversionService(this.beanFactory);
				}
			}
			if (this.conversionService == null && this.logger.isDebugEnabled()) {
				this.logger.debug("Unable to attempt conversion of Message payload types. Component '" +
						this.getComponentName() + "' has no explicit ConversionService reference, " +
						"and there is no 'integrationConversionService' bean within the context.");
			}
		}
		return this.conversionService;
	}

	protected void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Returns the {@link ApplicationContext#getId()} if the
	 * {@link ApplicationContext} is available.
	 * @return The id, or null if there is no application context.
	 */
	public String getApplicationContextId() {
		return this.applicationContext == null ? null : this.applicationContext.getId();
	}

	/**
	 * @return the applicationContext
	 */
	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * @see IntegrationContextUtils#getIntegrationProperties(BeanFactory)
	 * @return The global integration properties.
	 */
	protected Properties getIntegrationProperties() {
		return this.integrationProperties;
	}


	protected MessageBuilderFactory getMessageBuilderFactory() {
		if (this.messageBuilderFactory == null) {
			this.messageBuilderFactory = new DefaultMessageBuilderFactory();
		}
		return this.messageBuilderFactory;
	}

	public void setMessageBuilderFactory(MessageBuilderFactory messageBuilderFactory) {
		this.messageBuilderFactory = messageBuilderFactory;
	}

	/**
	 * @param  key    Integration property.
	 * @param  tClass the class to convert a value of Integration property.
	 * @param <T> The expected type of the property.
	 * @return the value of the Integration property converted to the provide type.
	 */
	protected <T> T getIntegrationProperty(String key, Class<T> tClass) {
		return this.defaultConversionService.convert(this.integrationProperties.getProperty(key), tClass);
	}

	@SuppressWarnings("unchecked")
	protected <T> T extractTypeIfPossible(Object targetObject, Class<T> expectedType) {
		if (targetObject == null) {
			return null;
		}
		if (expectedType.isAssignableFrom(targetObject.getClass())) {
			return (T) targetObject;
		}
		if (targetObject instanceof Advised) {
			TargetSource targetSource = ((Advised) targetObject).getTargetSource();
			if (targetSource == null) {
				return null;
			}
			try {
				return extractTypeIfPossible(targetSource.getTarget(), expectedType);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return (this.beanName != null) ? this.beanName : super.toString();
	}

}

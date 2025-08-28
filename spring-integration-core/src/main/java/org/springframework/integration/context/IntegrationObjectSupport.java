/*
 * Copyright 2002-present the original author or authors.
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

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.log.LogAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.channel.ChannelResolverUtils;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
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
 * @author Glenn Renfro
 */
public abstract class IntegrationObjectSupport implements ComponentSourceAware, NamedComponent,
		ApplicationContextAware, BeanFactoryAware, InitializingBean, ExpressionCapable {

	protected static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private static final IdGenerator ID_GENERATOR = new AlternativeJdkIdGenerator();

	protected final LogAccessor logger = new LogAccessor(getClass());

	@Nullable
	private DestinationResolver<MessageChannel> channelResolver;

	@SuppressWarnings("NullAway.Init")
	private String beanName;

	@Nullable
	private String componentName;

	@SuppressWarnings("NullAway.Init")
	private BeanFactory beanFactory;

	@Nullable
	private TaskScheduler taskScheduler;

	private IntegrationProperties integrationProperties = new IntegrationProperties();

	@Nullable
	private ConversionService conversionService;

	@SuppressWarnings("NullAway.Init")
	private ApplicationContext applicationContext;

	private MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

	private boolean messageBuilderFactoryExplicitlySet;

	@Nullable
	private Expression expression;

	@Nullable
	private Object beanSource;

	@Nullable
	private String beanDescription;

	private boolean initialized;

	@Override
	public final void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public String getBeanName() {
		return this.beanName;
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

	@Override
	public void setComponentSource(Object source) {
		this.beanSource = source;
	}

	@Override
	@Nullable
	public Object getComponentSource() {
		return this.beanSource;
	}

	@Override
	public void setComponentDescription(String description) {
		this.beanDescription = description;
	}

	@Override
	@Nullable
	public String getComponentDescription() {
		return this.beanDescription;
	}

	public String getBeanDescription() {
		StringBuilder sb =
				new StringBuilder("bean '")
						.append(this.beanName).append("'");
		String beanComponentName = getComponentName();
		if (!this.beanName.equals(beanComponentName)) {
			sb.append(" for component '").append(beanComponentName).append("'");
		}
		if (this.beanDescription != null) {
			sb.append("; defined in: '").append(this.beanDescription).append("'");
		}
		if (this.beanSource != null) {
			sb.append("; from source: '").append(this.beanSource).append("'");
		}
		return sb.toString();
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
	public @Nullable Expression getExpression() {
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
		if (!this.messageBuilderFactoryExplicitlySet) {
			this.messageBuilderFactory = IntegrationUtils.getMessageBuilderFactory(this.beanFactory);
		}
		if (this.beanSource == null && this.beanName != null
				&& this.beanFactory instanceof ConfigurableListableBeanFactory configurableListableBeanFactory
				&& configurableListableBeanFactory.containsBeanDefinition(this.beanName)) {
			BeanDefinition beanDefinition = configurableListableBeanFactory.getBeanDefinition(this.beanName);
			this.beanSource = beanDefinition.getSource();
			if (this.beanDescription == null) {
				this.beanDescription = beanDefinition.getResourceDescription();
			}
		}
		onInit();
		this.initialized = true;
	}

	/**
	 * Subclasses may implement this for initialization logic.
	 */
	protected void onInit() {
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

	/**
	 * Configure a {@link TaskScheduler} for those components which logic relies
	 * on the scheduled tasks.
	 * If not provided, falls back to the global {@code taskScheduler} bean
	 * in the application context, provided by the Spring Integration infrastructure.
	 * @param taskScheduler the {@link TaskScheduler} to use.
	 * @since 5.1.3
	 * @see #getTaskScheduler()
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "taskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	protected TaskScheduler getTaskScheduler() {
		TaskScheduler taskSchedulerToUse = this.taskScheduler;
		if (taskSchedulerToUse == null) {
			taskSchedulerToUse = IntegrationContextUtils.getTaskScheduler(this.beanFactory);
			this.taskScheduler = taskSchedulerToUse;
		}
		return taskSchedulerToUse;
	}

	protected DestinationResolver<MessageChannel> getChannelResolver() {
		DestinationResolver<MessageChannel> channelResolverToUse = this.channelResolver;
		if (channelResolverToUse == null) {
			channelResolverToUse = ChannelResolverUtils.getChannelResolver(this.beanFactory);
			this.channelResolver = channelResolverToUse;
		}
		return channelResolverToUse;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public ConversionService getConversionService() {
		ConversionService conversionServiceToUse = this.conversionService;
		if (conversionServiceToUse == null) {
			conversionServiceToUse = IntegrationUtils.getConversionService(this.beanFactory);
			if (conversionServiceToUse == null) {
				conversionServiceToUse = DefaultConversionService.getSharedInstance();
			}
			this.conversionService = conversionServiceToUse;
		}
		return conversionServiceToUse;
	}

	/**
	 * Returns the {@link ApplicationContext#getId()} if the
	 * {@link ApplicationContext} is available.
	 * @return The id, or null if there is no application context.
	 */
	public @Nullable String getApplicationContextId() {
		return this.applicationContext == null ? null : this.applicationContext.getId();
	}

	/**
	 * @return the applicationContext
	 */
	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * @return The global integration properties.
	 * @see IntegrationContextUtils#getIntegrationProperties(BeanFactory)
	 */
	protected IntegrationProperties getIntegrationProperties() {
		return this.integrationProperties;
	}

	protected MessageBuilderFactory getMessageBuilderFactory() {
		return this.messageBuilderFactory;
	}

	public void setMessageBuilderFactory(MessageBuilderFactory messageBuilderFactory) {
		this.messageBuilderFactory = messageBuilderFactory;
		this.messageBuilderFactoryExplicitlySet = true;
	}

	@Override
	public String toString() {
		return (this.beanName != null) ? getBeanDescription() : super.toString();
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T extractTypeIfPossible(@Nullable Object targetObject, Class<T> expectedType) {
		if (targetObject == null) {
			return null;
		}
		if (expectedType.isAssignableFrom(targetObject.getClass())) {
			return (T) targetObject;
		}
		else {
			return extractTypeIfPossible(AopProxyUtils.getSingletonTarget(targetObject), expectedType);
		}
	}

	public static UUID generateId() {
		return ID_GENERATOR.generateId();
	}

}

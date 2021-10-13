/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.monitor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.lang.model.SourceVersion;
import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.endpoint.IntegrationConsumer;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.history.MessageHistoryConfigurer;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.IntegrationInboundManagement;
import org.springframework.integration.support.management.IntegrationManagement;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.UnableToRegisterMBeanException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.MetricType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * <p>
 * MBean exporter for Spring Integration components in an existing application. Add an instance of this as a bean
 * definition in the same context as the components you need to monitor and all message channels and message handlers
 * will be exposed.
 * </p>
 * <p>
 * Channels will report metrics on send and receive (counts, rates, errors) and handlers will report metrics on
 * execution duration. Channels will be registered under their name (bean id), if explicit, or the last part of their
 * internal name (e.g. "nullChannel") if registered by the framework. A handler that is attached to an endpoint will be
 * registered with the endpoint name (bean id) if there is one, otherwise under the name of the input channel. Handler
 * object names contain a <code>bean</code> key that reports the source of the name: "endpoint" if the name is the
 * endpoint id; "anonymous" if it is the input channel; and "handler" as a fallback, where the object name is just the
 * <code>toString()</code> of the handler.
 * </p>
 * <p>
 * This component is itself an MBean, reporting attributes concerning the names and object names of the channels and
 * handlers. It doesn't register itself to avoid conflicts with the standard <code>&lt;context:mbean-export/&gt;</code>
 * from Spring (which should therefore be used any time you need to expose those features).
 * </p>
 *
 * @author Dave Syer
 * @author Helena Edelson
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Meherzad Lahewala
 */
@ManagedResource
public class IntegrationMBeanExporter extends MBeanExporter
		implements ApplicationContextAware, DestructionAwareBeanPostProcessor {

	public static final String DEFAULT_DOMAIN = IntegrationContextUtils.BASE_PACKAGE;

	private final IntegrationJmxAttributeSource attributeSource = new IntegrationJmxAttributeSource();

	private ApplicationContext applicationContext;

	private final Map<Object, AtomicLong> anonymousHandlerCounters = new HashMap<>();

	private final Map<Object, AtomicLong> anonymousSourceCounters = new HashMap<>();

	private final Map<String, IntegrationManagement> handlers = new HashMap<>();

	private final Map<String, IntegrationInboundManagement> sources = new HashMap<>();

	private final Map<IntegrationInboundManagement, ManageableLifecycle> sourceLifecycles = new HashMap<>();

	private final Set<Lifecycle> inboundLifecycleMessageProducers = new HashSet<>();

	private final Map<String, IntegrationManagement> channels = new HashMap<>();

	private final Map<Object, String> endpointsByMonitor = new HashMap<>();

	private final Map<Object, ObjectName> objectNames = new HashMap<>();

	private final Set<String> endpointNames = new HashSet<>();

	private final AtomicBoolean shuttingDown = new AtomicBoolean();

	private final Properties objectNameStaticProperties = new Properties();

	private final Set<Object> runtimeBeans = new HashSet<>();

	private final MetadataNamingStrategy defaultNamingStrategy =
			new MetadataNamingStrategy(this.attributeSource);

	private String domain = DEFAULT_DOMAIN;

	private String[] componentNamePatterns = { "*" };

	private volatile long shutdownDeadline;

	private volatile boolean singletonsInstantiated;

	public IntegrationMBeanExporter() {
		// Shouldn't be necessary, but to be on the safe side...
		setAutodetect(false);
		setNamingStrategy(this.defaultNamingStrategy);
		setAssembler(new MetadataMBeanInfoAssembler(this.attributeSource));
	}

	/**
	 * Static properties that will be added to all object names.
	 * @param objectNameStaticProperties the objectNameStaticProperties to set
	 */
	public void setObjectNameStaticProperties(Map<String, String> objectNameStaticProperties) {
		this.objectNameStaticProperties.putAll(objectNameStaticProperties);
	}

	/**
	 * The JMX domain to use for MBeans registered. Defaults to {@code spring.application} (which is useful in
	 * SpringSource HQ).
	 * @param domain the domain name to set
	 */
	public void setDefaultDomain(String domain) {
		this.domain = domain;
		this.defaultNamingStrategy.setDefaultDomain(domain);
	}

	/**
	 * Set the array of simple patterns for component names to register (defaults to '*').
	 * The pattern is applied to all components before they are registered, looking for a
	 * match on the 'name' property of the ObjectName. A MessageChannel and a
	 * MessageHandler (for instance) can share a name because they have a different type,
	 * so in that case they would either both be included or both excluded. Since version
	 * 4.2, a leading '!' negates the pattern match ('!foo*' means don't export components
	 * where the name matches the pattern 'foo*'). For components with names that match
	 * multiple patterns, the first pattern wins.
	 * @param componentNamePatterns the patterns.
	 */
	public void setComponentNamePatterns(String[] componentNamePatterns) {
		Assert.notEmpty(componentNamePatterns, "componentNamePatterns must not be empty");
		this.componentNamePatterns = Arrays.copyOf(componentNamePatterns, componentNamePatterns.length);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {

		Assert.notNull(applicationContext, "ApplicationContext may not be null");
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.attributeSource.setBeanFactory(beanFactory);
	}

	@Override
	public void afterSingletonsInstantiated() {
		populateMessageHandlers();
		populateMessageSources();
		populateMessageChannels();
		populateMessageProducers();

		super.afterSingletonsInstantiated();
		try {
			registerChannels();
			registerHandlers();
			registerSources();
			registerEndpoints();

			if (this.applicationContext
					.containsBean(IntegrationContextUtils.INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME)) {
				Object messageHistoryConfigurer = this.applicationContext
						.getBean(IntegrationContextUtils.INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME);
				if (messageHistoryConfigurer instanceof MessageHistoryConfigurer) {
					registerBeanInstance(messageHistoryConfigurer,
							IntegrationContextUtils.INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME);
				}
			}

			configureManagementConfigurer();

			this.singletonsInstantiated = true;
		}
		catch (RuntimeException e) {
			unregisterBeans();
			throw e;
		}
	}

	private void populateMessageHandlers() {
		Map<String, MessageHandler> messageHandlers = this.applicationContext
				.getBeansOfType(MessageHandler.class);
		for (Entry<String, MessageHandler> entry : messageHandlers.entrySet()) {

			String beanName = entry.getKey();
			MessageHandler bean = entry.getValue();
			if (this.handlerInAnonymousWrapper(bean) != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping " + beanName + " because it wraps another handler");
				}
				continue;
			}
			// If the handler is proxied, we have to extract the target to expose as an MBean.
			// The MetadataMBeanInfoAssembler does not support JDK dynamic proxies.
			MessageHandler monitor = (MessageHandler) extractTarget(bean);
			if (monitor instanceof IntegrationManagement) {
				this.handlers.put(beanName, (IntegrationManagement) monitor);
			}
		}
	}

	private void populateMessageSources() {
		this.applicationContext.getBeansOfType(
				IntegrationInboundManagement.class)
				.values()
				.stream()
				// If the source is proxied, we have to extract the target to expose as an MBean.
				// The MetadataMBeanInfoAssembler does not support JDK dynamic proxies.
				.map(this::extractTarget)
				.map(IntegrationInboundManagement.class::cast)
				.forEach(src -> this.sources.put(src.getComponentName(), src));
	}

	private void populateMessageChannels() {
		this.applicationContext.getBeansOfType(MessageChannel.class)
				.values()
				.stream()
				// If the channel is proxied, we have to extract the target to expose as an MBean.
				// The MetadataMBeanInfoAssembler does not support JDK dynamic proxies.
				.map(this::extractTarget)
				.filter(ch -> ch instanceof IntegrationManagement)
				.map(IntegrationManagement.class::cast)
				.forEach(ch -> this.channels.put(ch.getComponentName(), ch));
	}

	private void populateMessageProducers() {
		this.applicationContext.getBeansOfType(MessageProducer.class)
				.values()
				.stream()
				.filter(Lifecycle.class::isInstance)
				.forEach(this::registerProducer);
	}

	private void configureManagementConfigurer() {
		if (!this.applicationContext.containsBean(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME)) {
			IntegrationManagementConfigurer managementConfigurer = new IntegrationManagementConfigurer();
			managementConfigurer.setApplicationContext(this.applicationContext);
			managementConfigurer.setBeanName(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME);
			managementConfigurer.afterSingletonsInstantiated();
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (this.singletonsInstantiated) {
			try {
				if (bean instanceof MessageChannel) {
					MessageChannel monitor = (MessageChannel) extractTarget(bean);
					if (monitor instanceof IntegrationManagement) {
						this.channels.put(beanName, (IntegrationManagement) monitor);
						registerChannel((IntegrationManagement) monitor);
						this.runtimeBeans.add(bean);
					}
				}
				else if (bean instanceof MessageProducer && bean instanceof Lifecycle) {
					registerProducer((MessageProducer) bean);
					this.runtimeBeans.add(bean);
				}
				else if (bean instanceof AbstractEndpoint) {
					postProcessAbstractEndpoint(bean);
				}
			}
			catch (Exception e) {
				logger.error("Could not register an MBean for: " + beanName, e);
			}
		}
		return bean;
	}

	private void postProcessAbstractEndpoint(Object bean) {
		if (bean instanceof IntegrationConsumer) {
			IntegrationConsumer integrationConsumer = (IntegrationConsumer) bean;
			MessageHandler handler = integrationConsumer.getHandler();
			MessageHandler monitor = (MessageHandler) extractTarget(handler);
			if (monitor instanceof IntegrationManagement) {
				registerHandler((IntegrationManagement) monitor);
				this.handlers.put(((IntegrationManagement) monitor).getComponentName(),
						(IntegrationManagement) monitor);
				this.runtimeBeans.add(monitor);
			}
			return;
		}
		else if (bean instanceof SourcePollingChannelAdapter) {
			SourcePollingChannelAdapter pollingChannelAdapter = (SourcePollingChannelAdapter) bean;
			MessageSource<?> messageSource = pollingChannelAdapter.getMessageSource();
			if (messageSource instanceof IntegrationInboundManagement) {
				IntegrationInboundManagement monitor = (IntegrationInboundManagement) extractTarget(messageSource);
				registerSource(monitor);
				this.sourceLifecycles.put(monitor, pollingChannelAdapter);
				this.runtimeBeans.add(monitor);
				return;
			}
		}

		registerEndpoint((AbstractEndpoint) bean);
		this.runtimeBeans.add(bean);
	}

	private void registerProducer(MessageProducer messageProducer) {
		Lifecycle target = (Lifecycle) extractTarget(messageProducer);
		if (!(target instanceof AbstractMessageProducingHandler)) {
			this.inboundLifecycleMessageProducers.add(target);
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return bean instanceof AbstractMessageChannel || // NOSONAR
				bean instanceof AbstractMessageHandler ||
				bean instanceof AbstractMessageSource<?> ||
				(bean instanceof MessageProducer && bean instanceof Lifecycle) ||
				bean instanceof AbstractEndpoint;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (this.runtimeBeans.remove(bean)) {
			ObjectName objectName = this.objectNames.remove(bean);
			if (objectName != null) {
				doUnregister(objectName);
				if (bean instanceof AbstractEndpoint) {
					this.endpointNames.remove(((AbstractEndpoint) bean).getComponentName());
				}
				else {

					this.endpointsByMonitor.remove(bean);
					if (bean instanceof IntegrationManagement) {
						this.channels.remove(((NamedComponent) bean).getComponentName());
					}
					else if (bean instanceof IntegrationManagement) {
						this.handlers.remove(((NamedComponent) bean).getComponentName());
						this.endpointNames.remove(((NamedComponent) bean).getComponentName());
					}
					else if (bean instanceof IntegrationInboundManagement) {
						this.sources.remove(((NamedComponent) bean).getComponentName());
						this.endpointNames.remove(((NamedComponent) bean).getComponentName());
					}
				}
			}
			else if (bean instanceof MessageProducer && bean instanceof Lifecycle) {
				this.inboundLifecycleMessageProducers.remove(bean);
			}
		}
	}

	private MessageHandler handlerInAnonymousWrapper(final Object bean) {
		if (bean != null && bean.getClass().isAnonymousClass()) {
			final AtomicReference<MessageHandler> wrapped = new AtomicReference<>();
			ReflectionUtils.doWithFields(bean.getClass(), field -> {
				field.setAccessible(true);
				Object handler = field.get(bean);
				if (handler instanceof MessageHandler) {
					wrapped.set((MessageHandler) handler);
				}
			}, field -> wrapped.get() == null && field.getName().startsWith("val$"));
			return wrapped.get();
		}
		else {
			return null;
		}
	}

	/**
	 * Copy of private method in super class. Needed so we can avoid using the bean factory to extract the bean again,
	 * and risk it being a proxy (which it almost certainly is by now).
	 * @param bean the bean instance to register
	 * @param beanKey the bean name or human readable version if auto-generated
	 * @return the JMX object name of the MBean that was registered
	 */
	private ObjectName registerBeanInstance(Object bean, String beanKey) {
		try {
			ObjectName objectName = getObjectName(bean, beanKey);
			Object mbeanToExpose = null;
			if (isMBean(bean.getClass())) {
				mbeanToExpose = bean;
			}
			else {
				DynamicMBean adaptedBean = adaptMBeanIfPossible(bean);
				if (adaptedBean != null) {
					mbeanToExpose = adaptedBean;
				}
			}
			if (mbeanToExpose != null) {
				if (logger.isInfoEnabled()) {
					logger.info("Located MBean '" + beanKey + "': registering with JMX server as MBean [" + objectName
							+ "]");
				}
				doRegister(mbeanToExpose, objectName);
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Located managed bean '" + beanKey + "': registering with JMX server as MBean ["
							+ objectName + "]");
				}
				ModelMBean mbean = createAndConfigureMBean(bean, beanKey);
				doRegister(mbean, objectName);
				// injectNotificationPublisherIfNecessary(bean, mbean, objectName);
			}
			return objectName;
		}
		catch (JMException e) {
			throw new UnableToRegisterMBeanException("Unable to register MBean [" + bean + "] with key '" + beanKey
					+ "'", e);
		}
	}

	/**
	 * Shutdown active components.
	 * @param howLong The time to wait in total for all activities to complete
	 * in milliseconds.
	 */
	@ManagedOperation
	public void stopActiveComponents(long howLong) {
		if (!this.shuttingDown.compareAndSet(false, true)) {
			logger.error("Shutdown already in process");
			return;
		}
		this.shutdownDeadline = System.currentTimeMillis() + howLong;
		try {
			logger.debug("Running shutdown");
			doShutdown();
		}
		catch (Exception e) {
			logger.error("Orderly shutdown failed", e);
		}
	}

	/**
	 * Perform orderly shutdown - called or executed from
	 * {@link #stopActiveComponents(long)}.
	 */
	private void doShutdown() {
		try {
			orderlyShutdownCapableComponentsBefore();
			stopActiveChannels();
			stopMessageSources();
			stopInboundMessageProducers();
			// Wait any remaining time for messages to quiesce
			long timeLeft = this.shutdownDeadline - System.currentTimeMillis();
			if (timeLeft > 0) {
				try {
					Thread.sleep(timeLeft);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.error("Interrupted while waiting for quiesce");
				}
			}
			orderlyShutdownCapableComponentsAfter();
		}
		finally {
			this.shuttingDown.set(false);
		}
	}

	/**
	 * Stops all message sources - may cause interrupts.
	 */
	@ManagedOperation
	public void stopMessageSources() {
		for (Lifecycle source : this.sourceLifecycles.values()) {
			if (logger.isInfoEnabled()) {
				logger.info("Stopping message source " + source);
			}
			source.stop();
		}
	}

	/**
	 * Stops all inbound message producers (that are not {@link OrderlyShutdownCapable})
	 * - may cause interrupts.
	 */
	@ManagedOperation
	public void stopInboundMessageProducers() {
		for (Lifecycle producer : this.inboundLifecycleMessageProducers) {
			if (!(producer instanceof OrderlyShutdownCapable)) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping message producer " + producer);
				}
				producer.stop();
			}
		}
	}

	@ManagedOperation
	public void stopActiveChannels() {
		// Stop any "active" channels (JMS etc).
		for (IntegrationManagement metrics : this.channels.values()) {
			IntegrationManagement channel = metrics;
			if (channel instanceof Lifecycle) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping channel " + channel);
				}
				((Lifecycle) channel).stop();
			}
		}
	}

	protected final void orderlyShutdownCapableComponentsBefore() {
		logger.debug("Initiating stop OrderlyShutdownCapable components");
		Map<String, OrderlyShutdownCapable> components = this.applicationContext
				.getBeansOfType(OrderlyShutdownCapable.class);
		for (OrderlyShutdownCapable component : components.values()) {
			int n = component.beforeShutdown();
			if (logger.isInfoEnabled()) {
				logger.info("Initiated stop for component " + component + "; it reported " + n + " active messages");
			}
		}
		logger.debug("Initiated stop OrderlyShutdownCapable components");
	}

	protected final void orderlyShutdownCapableComponentsAfter() {
		logger.debug("Finalizing stop OrderlyShutdownCapable components");
		Map<String, OrderlyShutdownCapable> components = this.applicationContext
				.getBeansOfType(OrderlyShutdownCapable.class);
		for (OrderlyShutdownCapable component : components.values()) {
			int n = component.afterShutdown();
			if (logger.isInfoEnabled()) {
				logger.info("Finalized stop for component " + component + "; it reported " + n + " active messages");
			}
		}
		logger.debug("Finalized stop OrderlyShutdownCapable components");
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Count")
	public int getChannelCount() {
		return this.channels.size();
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageHandler Count")
	public int getHandlerCount() {
		return this.handlers.size();
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageSource Count")
	public int getSourceCount() {
		return this.sources.size();
	}

	@ManagedAttribute
	public String[] getHandlerNames() {
		return this.handlers.values().stream()
				.map(hand -> hand.getManagedName())
				.toArray(n -> new String[n]);
	}

	@Deprecated
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "No longer supported")
	public int getActiveHandlerCount() {
		return 0;
	}

	@Deprecated
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "No longer supported")
	public long getActiveHandlerCountLong() {
		return 0;
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Queued Message Count")
	public int getQueuedMessageCount() {
		return this.channels.values().stream()
				.filter(QueueChannel.class::isInstance)
				.map(QueueChannel.class::cast)
				.mapToInt(QueueChannel::getQueueSize)
				.sum();
	}

	@ManagedAttribute
	public String[] getChannelNames() {
		return this.channels.keySet().stream()
				.toArray(n -> new String[n]);
	}

	@Nullable
	@Deprecated
	public AbstractMessageHandler getHandlerMetrics(String name) {
		return null;
	}

	@Nullable
	public IntegrationManagement getHandler(String name) {
		return this.handlers.get(name);
	}

	@ManagedAttribute
	public String[] getSourceNames() {
		return this.sources.keySet().stream()
				.toArray(n -> new String[n]);
	}

	@Deprecated
	public IntegrationInboundManagement getSourceMetrics(String name) {
		return this.sources.get(name);
	}

	@Deprecated
	public IntegrationManagement getChannelMetrics(String name) {
		return this.channels.get(name);
	}

	public IntegrationInboundManagement getSource(String name) {
		return this.sources.get(name);
	}

	public IntegrationManagement getChannel(String name) {
		return this.channels.get(name);
	}

	private void registerChannels() {
		this.channels.values().forEach(this::registerChannel);
	}

	private void registerChannel(IntegrationManagement monitor) {
		String name = monitor.getComponentName();
		if (matches(this.componentNamePatterns, name)) {
			String beanKey = getChannelBeanKey(name);
			if (logger.isInfoEnabled()) {
				logger.info("Registering MessageChannel " + name);
			}
			ObjectName objectName = registerBeanNameOrInstance(monitor, beanKey);
			this.objectNames.put(monitor, objectName);
		}
	}

	private void registerHandlers() {
		this.handlers.values().forEach(this::registerHandler);

	}

	private void registerHandler(IntegrationManagement monitor2) {
		IntegrationManagement monitor = enhanceHandlerMonitor(monitor2);
		String name = monitor.getComponentName();
		if (!this.objectNames.containsKey(monitor2) && matches(this.componentNamePatterns, name)) {
			String beanKey = getHandlerBeanKey(monitor);
			if (logger.isInfoEnabled()) {
				logger.info("Registering MessageHandler " + name);
			}
			ObjectName objectName = registerBeanNameOrInstance(monitor, beanKey);
			this.objectNames.put(monitor2, objectName);
		}
	}

	private void registerSources() {
		this.sources.values().forEach(this::registerSource);
	}

	private void registerSource(IntegrationInboundManagement source) {
		IntegrationInboundManagement monitor = enhanceSourceMonitor(source);
		String name = monitor.getManagedName();
		if (!this.objectNames.containsKey(source) && matches(this.componentNamePatterns, name)) {
			String beanKey = getSourceBeanKey(monitor);
			if (logger.isInfoEnabled()) {
				logger.info("Registering MessageSource " + name);
			}
			ObjectName objectName = registerBeanNameOrInstance(monitor, beanKey);
			this.objectNames.put(source, objectName);
			Lifecycle lifecycle = this.sourceLifecycles.get(source);
			if (lifecycle != null) {
				beanKey = getEndpointBeanKey(source.getManagedName() + ".adapter", source.getManagedType());
				if (logger.isInfoEnabled()) {
					logger.info("Registering Endpoint " + beanKey);
				}
				this.objectNames.put(lifecycle, registerBeanNameOrInstance(lifecycle, beanKey));
			}
		}
	}

	private void registerEndpoints() {
		String[] names = this.applicationContext.getBeanNamesForType(AbstractEndpoint.class);
		for (String name : names) {
			if (!this.endpointsByMonitor.values().contains(name)) {
				AbstractEndpoint endpoint = this.applicationContext.getBean(name, AbstractEndpoint.class);
				registerEndpoint(endpoint);
			}
		}
	}

	private void registerEndpoint(AbstractEndpoint endpoint) {
		String beanKey;
		String name = endpoint.getComponentName();
		String source;
		if (name.startsWith('_' + IntegrationContextUtils.BASE_PACKAGE)) {
			name = getInternalComponentName(name);
			source = "internal";
		}
		else {
			source = "endpoint";
		}
		if (matches(this.componentNamePatterns, name)) {
			if (this.endpointNames.contains(name)) {
				int count = 0;
				String unique = name + "#" + count;
				while (this.endpointNames.contains(unique)) {
					unique = name + "#" + (++count);
				}
				name = unique;
			}
			this.endpointNames.add(name);
			beanKey = getEndpointBeanKey(name, source);
			ObjectName objectName = registerBeanInstance(endpoint, beanKey);
			this.objectNames.put(endpoint, objectName);
			if (logger.isInfoEnabled()) {
				logger.info("Registered endpoint without MessageSource: " + objectName);
			}
		}
	}

	/**
	 * Simple pattern match against the supplied patterns; also supports negated ('!')
	 * patterns. First match wins (positive or negative).
	 * @param patterns the patterns.
	 * @param name the name to match.
	 * @return true if positive match, false if no match or negative match.
	 */
	private boolean matches(String[] patterns, String name) {
		Boolean match = PatternMatchUtils.smartMatch(name, patterns);
		return match == null ? false : match;
	}

	private Object extractTarget(Object bean) {
		if (!(bean instanceof Advised)) {
			return bean;
		}
		Advised advised = (Advised) bean;
		try {
			return extractTarget(advised.getTargetSource().getTarget());
		}
		catch (Exception e) {
			logger.error("Could not extract target", e);
			return null;
		}
	}

	private String getChannelBeanKey(String channel) {
		String extra = "";
		if (channel.startsWith(IntegrationContextUtils.BASE_PACKAGE)) {
			extra = ",source=anonymous";
		}
		return String.format(this.domain + ":type=MessageChannel,name=%s%s" + getStaticNames(),
				quoteIfNecessary(channel), extra);
	}

	private String getHandlerBeanKey(IntegrationManagement monitor) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(this.domain + ":type=MessageHandler,name=%s,bean=%s" + getStaticNames(),
				quoteIfNecessary(monitor.getManagedName()), quoteIfNecessary(monitor.getManagedType()));
	}

	private String getSourceBeanKey(IntegrationInboundManagement monitor) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(this.domain + ":type=MessageSource,name=%s,bean=%s" + getStaticNames(),
				quoteIfNecessary(monitor.getManagedName()), quoteIfNecessary(monitor.getManagedType()));
	}

	private String getEndpointBeanKey(String name, String source) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(this.domain + ":type=ManagedEndpoint,name=%s,bean=%s" + getStaticNames(),
				quoteIfNecessary(name), source);
	}

	/*
	 * https://www.oracle.com/technetwork/java/javase/tech/best-practices-jsp-136021.html
	 *
	 * The set of characters in a value is also limited. If special characters may
	 * occur, it is recommended that the value be quoted, using ObjectName.quote. If
	 * the value for a given key is sometimes quoted, then it should always be quoted.
	 * By default, if a value is a string (rather than a number, say), then it should
	 * be quoted unless you are sure that it will never contain special characters.
	 */
	private String quoteIfNecessary(String name) {
		return SourceVersion.isName(name) ? name : ObjectName.quote(name);
	}

	private String getStaticNames() {
		if (this.objectNameStaticProperties.isEmpty()) {
			return "";
		}

		return ',' + this.objectNameStaticProperties.entrySet()
				.stream()
				.map((entry) -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining(","));
	}

	@SuppressWarnings("unlikely-arg-type")
	private IntegrationManagement enhanceHandlerMonitor(IntegrationManagement monitor2) {

		if (monitor2.getManagedName() != null && monitor2.getManagedType() != null) {
			return monitor2;
		}

		// Assignment algorithm and bean id, with bean id pulled reflectively out of enclosing endpoint if possible
		String[] names = this.applicationContext.getBeanNamesForType(IntegrationConsumer.class);

		String name = null;
		String endpointName = null;
		String source = "endpoint";
		IntegrationConsumer endpoint = null;

		for (String beanName : names) {
			endpoint = this.applicationContext.getBean(beanName, IntegrationConsumer.class);
			try {
				MessageHandler handler = endpoint.getHandler();
				if (handler.equals(monitor2) ||
						extractTarget(handlerInAnonymousWrapper(handler)).equals(monitor2)) {
					name = beanName;
					endpointName = beanName;
					break;
				}
			}
			catch (Exception e) {
				logger.trace("Could not get handler from bean = " + beanName, e);
				endpoint = null;
			}
		}

		IntegrationManagement messageHandlerMetrics =
				buildMessageHandlerMetrics(monitor2, name, source, endpoint);
		if (endpointName != null) {
			this.endpointsByMonitor.put(messageHandlerMetrics, endpointName);
		}
		return messageHandlerMetrics;
	}

	private IntegrationManagement buildMessageHandlerMetrics(
			IntegrationManagement monitor2,
			String name, String source, IntegrationConsumer endpoint) {

		IntegrationManagement result = monitor2;
		String managedType = source;
		String managedName = name;

		if (managedName != null && managedName.startsWith('_' + IntegrationContextUtils.BASE_PACKAGE)) {
			managedName = getInternalComponentName(managedName);
			managedType = "internal";
		}
		if (managedName != null && name.startsWith(IntegrationContextUtils.BASE_PACKAGE)) {
			MessageChannel inputChannel = endpoint.getInputChannel();
			if (inputChannel != null) {
				managedName = buildAnonymousManagedName(this.anonymousHandlerCounters, inputChannel);
				managedType = "anonymous";
			}
		}

		if (managedName == null) {
			managedName = ((NamedComponent) monitor2).getComponentName();
			if (managedName == null) {
				managedName = monitor2.toString();
			}
			managedType = "handler";
		}

		result.setManagedType(managedType);
		result.setManagedName(managedName);
		return result;
	}

	private String buildAnonymousManagedName(Map<Object, AtomicLong> anonymousCache, MessageChannel messageChannel) {
		AtomicLong count = anonymousCache.computeIfAbsent(messageChannel, (key) -> new AtomicLong());
		long total = count.incrementAndGet();
		/*
		 * Short hack to makes sure object names are unique if more than one endpoint has the same input
		 * channel
		 */

		String channelName =
				messageChannel instanceof NamedComponent
						? ((NamedComponent) messageChannel).getBeanName()
						: messageChannel.toString();

		return channelName + (total > 1 ? "#" + total : "");
	}

	private String getInternalComponentName(String name) {
		return name.substring(('_' + IntegrationContextUtils.BASE_PACKAGE).length() + 1);
	}

	private IntegrationInboundManagement enhanceSourceMonitor(IntegrationInboundManagement source2) {

		if (source2.getManagedName() != null) {
			return source2;
		}

		String endpointName = null;
		String source = "endpoint";
		AbstractEndpoint endpoint = getEndpointForMonitor(source2);
		this.sourceLifecycles.put(source2, endpoint);

		if (endpoint != null) {
			endpointName = endpoint.getBeanName();
		}
		if (endpointName != null && endpointName.startsWith('_' + IntegrationContextUtils.BASE_PACKAGE)) {
			endpointName = getInternalComponentName(endpointName);
			source = "internal";
		}

		IntegrationInboundManagement messageSourceMetrics =
				buildMessageSourceMetricsIfAny(source2, endpointName, source, endpoint);
		if (endpointName != null) {
			this.endpointsByMonitor.put(messageSourceMetrics, endpointName);
		}
		return messageSourceMetrics;
	}

	@SuppressWarnings("unlikely-arg-type")
	private AbstractEndpoint getEndpointForMonitor(IntegrationInboundManagement source2) {

		for (AbstractEndpoint endpoint : this.applicationContext.getBeansOfType(AbstractEndpoint.class).values()) {
			Object target = null;
			if (source2 instanceof MessagingGatewaySupport && endpoint.equals(source2)) {
				target = source2;
			}
			else if (endpoint instanceof SourcePollingChannelAdapter) {
				target = ((SourcePollingChannelAdapter) endpoint).getMessageSource();
			}
			if (source2.equals(target)) {
				return endpoint;
			}
		}
		return null;
	}

	private IntegrationInboundManagement buildMessageSourceMetricsIfAny(
			IntegrationInboundManagement source2, String name,
			String source, Object endpoint) {

		IntegrationInboundManagement result = source2;
		String managedType = source;
		String managedName = name;

		if (managedName != null && managedName.startsWith(IntegrationContextUtils.BASE_PACKAGE)) {
			Object target = endpoint;
			if (endpoint instanceof Advised) {
				TargetSource targetSource = ((Advised) endpoint).getTargetSource();
				try {
					target = targetSource.getTarget();
				}
				catch (Exception e) {
					logger.error("Could not get handler from bean = " + managedName);
				}
			}

			MessageChannel outputChannel = null;
			if (target instanceof MessagingGatewaySupport) {
				outputChannel = ((MessagingGatewaySupport) target).getRequestChannel();
			}
			else if (target instanceof SourcePollingChannelAdapter) {
				outputChannel = ((SourcePollingChannelAdapter) target).getOutputChannel();
			}

			if (outputChannel != null) {
				managedName = buildAnonymousManagedName(this.anonymousSourceCounters, outputChannel);
				managedType = "anonymous";
			}
		}

		if (managedName == null) {
			managedName = result.toString();
			managedType = "source";
		}

		result.setManagedType(managedType);
		result.setManagedName(managedName);
		return result;
	}

}

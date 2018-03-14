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

package org.springframework.integration.monitor;

import java.lang.reflect.Field;
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

import javax.management.DynamicMBean;
import javax.management.JMException;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.history.MessageHistoryConfigurer;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.LifecycleMessageHandlerMetrics;
import org.springframework.integration.support.management.LifecycleMessageSourceManagement;
import org.springframework.integration.support.management.LifecycleMessageSourceMetrics;
import org.springframework.integration.support.management.LifecycleTrackableMessageHandlerMetrics;
import org.springframework.integration.support.management.LifecycleTrackableMessageSourceManagement;
import org.springframework.integration.support.management.LifecycleTrackableMessageSourceMetrics;
import org.springframework.integration.support.management.MappingMessageRouterManagement;
import org.springframework.integration.support.management.MessageChannelMetrics;
import org.springframework.integration.support.management.MessageHandlerMetrics;
import org.springframework.integration.support.management.MessageSourceManagement;
import org.springframework.integration.support.management.MessageSourceMetrics;
import org.springframework.integration.support.management.PollableChannelManagement;
import org.springframework.integration.support.management.RouterMetrics;
import org.springframework.integration.support.management.Statistics;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.integration.support.management.TrackableRouterMetrics;
import org.springframework.integration.support.utils.PatternMatchUtils;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.UnableToRegisterMBeanException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.MetricType;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

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
@org.springframework.jmx.export.annotation.ManagedResource
public class IntegrationMBeanExporter extends MBeanExporter implements ApplicationContextAware,
		EmbeddedValueResolverAware {

	private static final Log logger = LogFactory.getLog(IntegrationMBeanExporter.class);

	public static final String DEFAULT_DOMAIN = "org.springframework.integration";

	private final IntegrationJmxAttributeSource attributeSource = new IntegrationJmxAttributeSource();

	private ApplicationContext applicationContext;

	private final Map<Object, AtomicLong> anonymousHandlerCounters = new HashMap<>();

	private final Map<Object, AtomicLong> anonymousSourceCounters = new HashMap<>();

	private final Set<MessageHandlerMetrics> handlers = new HashSet<>();

	private final Set<MessageSourceMetrics> sources = new HashSet<>();

	private final Set<Lifecycle> inboundLifecycleMessageProducers = new HashSet<>();

	private final Set<MessageChannelMetrics> channels = new HashSet<>();

	private final Map<String, MessageChannelMetrics> allChannelsByName = new HashMap<>();

	private final Map<String, MessageHandlerMetrics> allHandlersByName = new HashMap<>();

	private final Map<String, MessageSourceMetrics> allSourcesByName = new HashMap<>();

	private final Map<String, String> beansByEndpointName = new HashMap<>();

	private String domain = DEFAULT_DOMAIN;

	private final Properties objectNameStaticProperties = new Properties();

	private final MetadataNamingStrategy defaultNamingStrategy =
			new IntegrationMetadataNamingStrategy(this.attributeSource);

	private String[] componentNamePatterns = { "*" };

	private IntegrationManagementConfigurer managementConfigurer;

	private volatile long shutdownDeadline;

	private final AtomicBoolean shuttingDown = new AtomicBoolean();


	public IntegrationMBeanExporter() {
		super();
		// Shouldn't be necessary, but to be on the safe side...
		setAutodetect(false);
		setNamingStrategy(this.defaultNamingStrategy);
		setAssembler(new IntegrationMetadataMBeanInfoAssembler(this.attributeSource));
	}

	/**
	 * Static properties that will be added to all object names.
	 *
	 * @param objectNameStaticProperties the objectNameStaticProperties to set
	 */
	public void setObjectNameStaticProperties(Map<String, String> objectNameStaticProperties) {
		this.objectNameStaticProperties.putAll(objectNameStaticProperties);
	}

	/**
	 * The JMX domain to use for MBeans registered. Defaults to <code>spring.application</code> (which is useful in
	 * SpringSource HQ).
	 *
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
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.attributeSource.setValueResolver(resolver);
	}

	@Override
	public void afterSingletonsInstantiated() {
		Map<String, MessageHandlerMetrics> messageHandlers =
				this.applicationContext.getBeansOfType(MessageHandlerMetrics.class);
		for (Entry<String, MessageHandlerMetrics> entry : messageHandlers.entrySet()) {
			String beanName = entry.getKey();
			MessageHandlerMetrics bean = entry.getValue();
			if (this.handlerInAnonymousWrapper(bean) != null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Skipping " + beanName + " because it wraps another handler");
				}
				continue;
			}
			// If the handler is proxied, we have to extract the target to expose as an MBean.
			// The MetadataMBeanInfoAssembler does not support JDK dynamic proxies.
			MessageHandlerMetrics monitor = (MessageHandlerMetrics) extractTarget(bean);
			this.handlers.add(monitor);
		}

		Map<String, MessageSourceMetrics> messageSources =
				this.applicationContext.getBeansOfType(MessageSourceMetrics.class);
		for (Entry<String, MessageSourceMetrics> entry : messageSources.entrySet()) {
			// If the source is proxied, we have to extract the target to expose as an MBean.
			// The MetadataMBeanInfoAssembler does not support JDK dynamic proxies.
			MessageSourceMetrics monitor = (MessageSourceMetrics) extractTarget(entry.getValue());
			this.sources.add(monitor);
		}

		Map<String, MessageChannelMetrics> messageChannels =
				this.applicationContext.getBeansOfType(MessageChannelMetrics.class);
		for (Entry<String, MessageChannelMetrics> entry : messageChannels.entrySet()) {
			// If the channel is proxied, we have to extract the target to expose as an MBean.
			// The MetadataMBeanInfoAssembler does not support JDK dynamic proxies.
			MessageChannelMetrics monitor = (MessageChannelMetrics) extractTarget(entry.getValue());
			this.channels.add(monitor);
		}
		Map<String, MessageProducer> messageProducers =
				this.applicationContext.getBeansOfType(MessageProducer.class);
		for (Entry<String, MessageProducer> entry : messageProducers.entrySet()) {
			MessageProducer messageProducer = entry.getValue();
			if (messageProducer instanceof Lifecycle) {
				Lifecycle target = (Lifecycle) extractTarget(messageProducer);
				if (!(target instanceof AbstractMessageProducingHandler)) {
					this.inboundLifecycleMessageProducers.add(target);
				}
			}
		}
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
			if (!this.applicationContext.containsBean(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME)) {
				this.managementConfigurer = new IntegrationManagementConfigurer();
				this.managementConfigurer.setDefaultCountsEnabled(true);
				this.managementConfigurer.setDefaultStatsEnabled(true);
				this.managementConfigurer.setApplicationContext(this.applicationContext);
				this.managementConfigurer.setBeanName(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME);
				this.managementConfigurer.afterSingletonsInstantiated();
			}
			else {
				this.managementConfigurer =
						this.applicationContext.getBean(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME,
								IntegrationManagementConfigurer.class);
			}
		}
		catch (RuntimeException e) {
			unregisterBeans();
			throw e;
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
	 *
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

	@Override
	public void destroy() {
		super.destroy();
		for (MessageChannelMetrics monitor : this.channels) {
			logger.info("Summary on shutdown: " + monitor);
		}
		for (MessageHandlerMetrics monitor : this.handlers) {
			logger.info("Summary on shutdown: " + monitor);
		}
		for (MessageSourceMetrics monitor : this.sources) {
			logger.info("Summary on shutdown: " + monitor);
		}
	}

	/**
	 * Shutdown active components.
	 *
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
		for (Entry<String, MessageSourceMetrics> entry : this.allSourcesByName.entrySet()) {
			MessageSourceMetrics sourceMetrics = entry.getValue();
			if (sourceMetrics instanceof Lifecycle) {
				if (logger.isInfoEnabled()) {
					logger.info("Stopping message source " + sourceMetrics);
				}
				((Lifecycle) sourceMetrics).stop();
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Message source " + sourceMetrics + " cannot be stopped");
				}
			}
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
		for (Entry<String, MessageChannelMetrics> entry : this.allChannelsByName.entrySet()) {
			MessageChannelMetrics metrics = entry.getValue();
			MessageChannel channel = (MessageChannel) metrics;
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
		for (Entry<String, OrderlyShutdownCapable> componentEntry : components.entrySet()) {
			OrderlyShutdownCapable component = componentEntry.getValue();
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
		for (Entry<String, OrderlyShutdownCapable> componentEntry : components.entrySet()) {
			OrderlyShutdownCapable component = componentEntry.getValue();
			int n = component.afterShutdown();
			if (logger.isInfoEnabled()) {
				logger.info("Finalized stop for component " + component + "; it reported " + n + " active messages");
			}
		}
		logger.debug("Finalized stop OrderlyShutdownCapable components");
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Count")
	public int getChannelCount() {
		return this.managementConfigurer.getChannelNames().length;
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageHandler Count")
	public int getHandlerCount() {
		return this.managementConfigurer.getHandlerNames().length;
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageSource Count")
	public int getSourceCount() {
		return this.managementConfigurer.getSourceNames().length;
	}

	@ManagedAttribute
	public String[] getHandlerNames() {
		return this.managementConfigurer.getHandlerNames();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Active Handler Count")
	public int getActiveHandlerCount() {
		return (int) getActiveHandlerCountLong();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Active Handler Count")
	public long getActiveHandlerCountLong() {
		int count = 0;
		for (MessageHandlerMetrics monitor : this.handlers) {
			count += monitor.getActiveCountLong();
		}
		return count;
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Queued Message Count")
	public int getQueuedMessageCount() {
		int count = 0;
		for (MessageChannelMetrics monitor : this.channels) {
			if (monitor instanceof QueueChannel) {
				count += ((QueueChannel) monitor).getQueueSize();
			}
		}
		return count;
	}

	@ManagedAttribute
	public String[] getChannelNames() {
		return this.managementConfigurer.getChannelNames();
	}

	public MessageHandlerMetrics getHandlerMetrics(String name) {
		return this.managementConfigurer.getHandlerMetrics(name);
	}

	public Statistics getHandlerDuration(String name) {
		MessageHandlerMetrics handlerMetrics = getHandlerMetrics(name);
		return handlerMetrics != null ? handlerMetrics.getDuration() : null;
	}

	@ManagedAttribute
	public String[] getSourceNames() {
		return this.managementConfigurer.getSourceNames();
	}

	public MessageSourceMetrics getSourceMetrics(String name) {
		return this.managementConfigurer.getSourceMetrics(name);
	}

	public int getSourceMessageCount(String name) {
		return (int) getSourceMessageCountLong(name);
	}

	public long getSourceMessageCountLong(String name) {
		MessageSourceMetrics sourceMetrics = getSourceMetrics(name);
		return sourceMetrics != null ? sourceMetrics.getMessageCountLong() : -1;
	}

	public MessageChannelMetrics getChannelMetrics(String name) {
		return this.managementConfigurer.getChannelMetrics(name);
	}

	public int getChannelSendCount(String name) {
		return (int) getChannelSendCountLong(name);
	}

	public long getChannelSendCountLong(String name) {
		MessageChannelMetrics channelMetrics = getChannelMetrics(name);
		return channelMetrics != null ? channelMetrics.getSendCountLong() : -1;
	}

	public int getChannelSendErrorCount(String name) {
		return (int) getChannelSendErrorCountLong(name);
	}

	public long getChannelSendErrorCountLong(String name) {
		MessageChannelMetrics channelMetrics = getChannelMetrics(name);
		return channelMetrics != null ? channelMetrics.getSendErrorCountLong() : -1;
	}

	public int getChannelReceiveCount(String name) {
		return (int) getChannelReceiveCountLong(name);
	}

	public long getChannelReceiveCountLong(String name) {
		MessageChannelMetrics channelMetrics = getChannelMetrics(name);
		if (channelMetrics instanceof PollableChannelManagement) {
			return ((PollableChannelManagement) channelMetrics).getReceiveCountLong();
		}
		return -1;
	}

	@ManagedOperation
	public Statistics getChannelSendRate(String name) {
		MessageChannelMetrics channelMetrics = getChannelMetrics(name);
		return channelMetrics != null ? channelMetrics.getSendRate() : null;
	}

	public Statistics getChannelErrorRate(String name) {
		MessageChannelMetrics channelMetrics = getChannelMetrics(name);
		return channelMetrics != null ? channelMetrics.getErrorRate() : null;
	}

	private void registerChannels() {
		for (MessageChannelMetrics monitor : this.channels) {
			String name = ((NamedComponent) monitor).getComponentName();
			this.allChannelsByName.put(name, monitor);
			if (!matches(this.componentNamePatterns, name)) {
				continue;
			}

			String beanKey = getChannelBeanKey(name);
			if (logger.isInfoEnabled()) {
				logger.info("Registering MessageChannel " + name);
			}
			registerBeanNameOrInstance(monitor, beanKey);
		}
	}

	private void registerHandlers() {
		for (MessageHandlerMetrics handler : this.handlers) {
			MessageHandlerMetrics monitor = enhanceHandlerMonitor(handler);
			String name = monitor.getManagedName();
			this.allHandlersByName.put(name, monitor);
			if (!matches(this.componentNamePatterns, name)) {
				continue;
			}

			String beanKey = getHandlerBeanKey(monitor);
			if (logger.isInfoEnabled()) {
				logger.info("Registering MessageHandler " + name);
			}
			registerBeanNameOrInstance(monitor, beanKey);
		}
	}

	private void registerSources() {
		for (MessageSourceMetrics source : this.sources) {
			MessageSourceMetrics monitor = enhanceSourceMonitor(source);
			String name = monitor.getManagedName();
			this.allSourcesByName.put(name, monitor);
			if (!matches(this.componentNamePatterns, name)) {
				continue;
			}

			String beanKey = getSourceBeanKey(monitor);
			if (logger.isInfoEnabled()) {
				logger.info("Registering MessageSource " + name);
			}
			registerBeanNameOrInstance(monitor, beanKey);
		}
	}

	private void registerEndpoints() {
		String[] names = this.applicationContext.getBeanNamesForType(AbstractEndpoint.class);
		Set<String> endpointNames = new HashSet<>();
		for (String name : names) {
			if (!this.beansByEndpointName.values().contains(name)) {
				AbstractEndpoint endpoint = this.applicationContext.getBean(name, AbstractEndpoint.class);
				String beanKey;
				name = endpoint.getComponentName();
				String source;
				if (name.startsWith("_org.springframework.integration")) {
					name = getInternalComponentName(name);
					source = "internal";
				}
				else {
					name = endpoint.getComponentName();
					source = "endpoint";
				}
				if (!matches(this.componentNamePatterns, name)) {
					continue;
				}
				if (endpointNames.contains(name)) {
					int count = 0;
					String unique = name + "#" + count;
					while (endpointNames.contains(unique)) {
						unique = name + "#" + (++count);
					}
					name = unique;
				}
				endpointNames.add(name);
				beanKey = getEndpointBeanKey(endpoint, name, source);
				ObjectName objectName = registerBeanInstance(new ManagedEndpoint(endpoint), beanKey);
				if (logger.isInfoEnabled()) {
					logger.info("Registered endpoint without MessageSource: " + objectName);
				}
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
		if (advised.getTargetSource() == null) {
			return null;
		}
		try {
			return extractTarget(advised.getTargetSource().getTarget());
		}
		catch (Exception e) {
			logger.error("Could not extract target", e);
			return null;
		}
	}

	private String getChannelBeanKey(String channel) {
		String name = "" + channel;
		if (name.startsWith("org.springframework.integration")) {
			name = name + ",source=anonymous";
		}
		return String.format(this.domain + ":type=MessageChannel,name=%s" + getStaticNames(), name);
	}

	private String getHandlerBeanKey(MessageHandlerMetrics handler) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(this.domain + ":type=MessageHandler,name=%s,bean=%s" + getStaticNames(),
				handler.getManagedName(), handler.getManagedType());
	}

	private String getSourceBeanKey(MessageSourceMetrics source) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(this.domain + ":type=MessageSource,name=%s,bean=%s" + getStaticNames(),
				source.getManagedName(), source.getManagedType());
	}

	private String getEndpointBeanKey(AbstractEndpoint endpoint, String name, String source) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(this.domain + ":type=ManagedEndpoint,name=%s,bean=%s" + getStaticNames(), name, source);
	}

	private String getStaticNames() {
		if (this.objectNameStaticProperties.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();

		for (Entry<Object, Object> entry : this.objectNameStaticProperties.entrySet()) {
			builder.append("," + entry.getKey() + "=" + entry.getValue());
		}
		return builder.toString();
	}

	private MessageHandlerMetrics enhanceHandlerMonitor(MessageHandlerMetrics monitor) {

		MessageHandlerMetrics result = monitor;

		if (monitor.getManagedName() != null && monitor.getManagedType() != null) {
			return monitor;
		}

		// Assignment algorithm and bean id, with bean id pulled reflectively out of enclosing endpoint if possible
		String[] names = this.applicationContext.getBeanNamesForType(AbstractEndpoint.class);

		String name = null;
		String endpointName = null;
		String source = "endpoint";
		Object endpoint = null;

		for (String beanName : names) {
			endpoint = this.applicationContext.getBean(beanName);
			try {
				Object field = extractTarget(getField(endpoint, "handler"));
				if (field == monitor ||
						this.extractTarget(this.handlerInAnonymousWrapper(field)) == monitor) {
					name = beanName;
					endpointName = beanName;
					break;
				}
			}
			catch (Exception e) {
				logger.trace("Could not get handler from bean = " + beanName);
			}
		}
		if (name != null && endpoint != null && name.startsWith("_org.springframework.integration")) {
			name = getInternalComponentName(name);
			source = "internal";
		}
		if (name != null && endpoint != null && name.startsWith("org.springframework.integration")) {
			Object target = endpoint;
			if (endpoint instanceof Advised) {
				TargetSource targetSource = ((Advised) endpoint).getTargetSource();
				if (targetSource != null) {
					try {
						target = targetSource.getTarget();
					}
					catch (Exception e) {
						logger.error("Could not get handler from bean = " + name);
					}
				}
			}
			Object field = getField(target, "inputChannel");
			if (field != null) {
				if (!this.anonymousHandlerCounters.containsKey(field)) {
					this.anonymousHandlerCounters.put(field, new AtomicLong());
				}
				AtomicLong count = this.anonymousHandlerCounters.get(field);
				long total = count.incrementAndGet();
				String suffix = "";
				/*
				 * Short hack to makes sure object names are unique if more than one endpoint has the same input channel
				 */
				if (total > 1) {
					suffix = "#" + total;
				}
				name = field + suffix;
				source = "anonymous";
			}
		}

		if (endpoint instanceof Lifecycle) {
			// Wrap the monitor in a lifecycle so it exposes the start/stop operations
			if (monitor instanceof MappingMessageRouterManagement) {
				if (monitor instanceof TrackableComponent) {
					result = new TrackableRouterMetrics((Lifecycle) endpoint, (MappingMessageRouterManagement) monitor);
				}
				else {
					result = new RouterMetrics((Lifecycle) endpoint, (MappingMessageRouterManagement) monitor);
				}
			}
			else {
				if (monitor instanceof TrackableComponent) {
					result = new LifecycleTrackableMessageHandlerMetrics((Lifecycle) endpoint, monitor);
				}
				else {
					result = new LifecycleMessageHandlerMetrics((Lifecycle) endpoint, monitor);
				}
			}
		}

		if (name == null) {
			if (monitor instanceof NamedComponent) {
				name = ((NamedComponent) monitor).getComponentName();
			}
			if (name == null) {
				name = monitor.toString();
			}
			source = "handler";
		}

		if (endpointName != null) {
			this.beansByEndpointName.put(name, endpointName);
		}

		monitor.setManagedType(source);
		monitor.setManagedName(name);

		return result;

	}

	private String getInternalComponentName(String name) {
		return name.substring("_org.springframework.integration".length() + 1);
	}

	private MessageSourceMetrics enhanceSourceMonitor(MessageSourceMetrics monitor) {

		MessageSourceMetrics result = monitor;

		if (monitor.getManagedName() != null) {
			return monitor;
		}

		// Assignment algorithm and bean id, with bean id pulled reflectively out of enclosing endpoint if possible
		String[] names = this.applicationContext.getBeanNamesForType(AbstractEndpoint.class);

		String name = null;
		String endpointName = null;
		String source = "endpoint";
		Object endpoint = null;

		for (String beanName : names) {
			endpoint = this.applicationContext.getBean(beanName);
			Object field = null;
			if (monitor instanceof MessagingGatewaySupport && endpoint == monitor) {
				field = monitor;
			}
			else {
				try {
					field = extractTarget(getField(endpoint, "source"));
				}
				catch (Exception e) {
					logger.trace("Could not get source from bean = " + beanName);
				}
			}

			if (field == monitor) {
				name = beanName;
				endpointName = beanName;
				break;
			}
		}
		if (endpointName == null) {
			endpoint = null;
		}
		if (name != null && endpoint != null && name.startsWith("_org.springframework.integration")) {
			name = getInternalComponentName(name);
			source = "internal";
		}
		if (name != null && endpoint != null && name.startsWith("org.springframework.integration")) {
			Object target = endpoint;
			if (endpoint instanceof Advised) {
				TargetSource targetSource = ((Advised) endpoint).getTargetSource();
				if (targetSource != null) {
					try {
						target = targetSource.getTarget();
					}
					catch (Exception e) {
						logger.error("Could not get handler from bean = " + name);
					}
				}
			}

			Object outputChannel = null;
			if (target instanceof MessagingGatewaySupport) {
				outputChannel = ((MessagingGatewaySupport) target).getRequestChannel();
			}
			else {
				outputChannel = getField(target, "outputChannel");
			}

			if (outputChannel != null) {
				if (!this.anonymousSourceCounters.containsKey(outputChannel)) {
					this.anonymousSourceCounters.put(outputChannel, new AtomicLong());
				}
				AtomicLong count = this.anonymousSourceCounters.get(outputChannel);
				long total = count.incrementAndGet();
				String suffix = "";
				/*
				 * Short hack to makes sure object names are unique if more than one endpoint has the same input channel
				 */
				if (total > 1) {
					suffix = "#" + total;
				}
				name = outputChannel + suffix;
				source = "anonymous";
			}
		}

		if (endpoint instanceof Lifecycle) {
			// Wrap the monitor in a lifecycle so it exposes the start/stop operations
			if (endpoint instanceof TrackableComponent) {
				if (monitor instanceof MessageSourceManagement) {
					result = new LifecycleTrackableMessageSourceManagement((Lifecycle) endpoint,
							(MessageSourceManagement) monitor);
				}
				else {
					result = new LifecycleTrackableMessageSourceMetrics((Lifecycle) endpoint, monitor);
				}
			}
			else {
				if (monitor instanceof MessageSourceManagement) {
					result = new LifecycleMessageSourceManagement((Lifecycle) endpoint,
							(MessageSourceManagement) monitor);
				}
				else {
					result = new LifecycleMessageSourceMetrics((Lifecycle) endpoint, monitor);
				}
			}
		}

		if (name == null) {
			name = monitor.toString();
			source = "source";
		}

		if (endpointName != null) {
			this.beansByEndpointName.put(name, endpointName);
		}

		monitor.setManagedType(source);
		monitor.setManagedName(name);

		return result;
	}

	private static Object getField(Object target, String name) {
		Assert.notNull(target, "Target object must not be null");
		Field field = ReflectionUtils.findField(target.getClass(), name);
		if (field == null) {
			throw new IllegalArgumentException("Could not find field [" + name + "] on target [" + target + "]");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Getting field [" + name + "] from target [" + target + "]");
		}
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, target);
	}

}

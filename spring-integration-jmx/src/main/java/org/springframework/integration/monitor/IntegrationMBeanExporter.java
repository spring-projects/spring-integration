/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
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
import org.springframework.beans.annotation.AnnotationBeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.management.AbstractMessageChannelMetrics;
import org.springframework.integration.channel.management.MessageChannelMetrics;
import org.springframework.integration.channel.management.PollableChannelManagement;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.management.MessageSourceMetrics;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.management.AbstractMessageHandlerMetrics;
import org.springframework.integration.handler.management.MessageHandlerMetrics;
import org.springframework.integration.history.MessageHistoryConfigurer;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.router.MappingMessageRouterManagement;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.integration.support.management.ConfigurableMetricsAware;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.management.Statistics;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.UnableToRegisterMBeanException;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.MetricType;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;
import org.springframework.util.ReflectionUtils.FieldFilter;
import org.springframework.util.StringUtils;
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
 */
@ManagedResource
@IntegrationManagedResource
public class IntegrationMBeanExporter extends MBeanExporter implements ApplicationContextAware,
		EmbeddedValueResolverAware {

	private static final Log logger = LogFactory.getLog(IntegrationMBeanExporter.class);

	public static final String DEFAULT_DOMAIN = "org.springframework.integration";

	private final AnnotationJmxAttributeSource attributeSource = new IntegrationJmxAttributeSource();

	private ApplicationContext applicationContext;

	private final Map<Object, AtomicLong> anonymousHandlerCounters = new HashMap<Object, AtomicLong>();

	private final Map<Object, AtomicLong> anonymousSourceCounters = new HashMap<Object, AtomicLong>();

	private final Set<MessageHandlerMetrics> handlers = new HashSet<MessageHandlerMetrics>();

	private final Set<MessageSourceMetrics> sources = new HashSet<MessageSourceMetrics>();

	private final Set<Lifecycle> inboundLifecycleMessageProducers = new HashSet<Lifecycle>();

	private final Set<MessageChannelMetrics> channels = new HashSet<MessageChannelMetrics>();

	private final Map<String, MessageChannelMetrics> channelsByName = new HashMap<String, MessageChannelMetrics>();

	private final Map<String, MessageHandlerMetrics> handlersByName = new HashMap<String, MessageHandlerMetrics>();

	private final Map<String, MessageSourceMetrics> sourcesByName = new HashMap<String, MessageSourceMetrics>();

	private final Map<String, MessageChannelMetrics> allChannelsByName = new HashMap<String, MessageChannelMetrics>();

	private final Map<String, MessageHandlerMetrics> allHandlersByName = new HashMap<String, MessageHandlerMetrics>();

	private final Map<String, MessageSourceMetrics> allSourcesByName = new HashMap<String, MessageSourceMetrics>();

	private final Map<String, String> beansByEndpointName = new HashMap<String, String>();

	private String domain = DEFAULT_DOMAIN;

	private final Properties objectNameStaticProperties = new Properties();

	private final MetadataMBeanInfoAssembler assembler = new MetadataMBeanInfoAssembler(attributeSource);

	private final MetadataNamingStrategy defaultNamingStrategy = new MetadataNamingStrategy(attributeSource);

	private String[] componentNamePatterns = { "*" };

	private String[] enabledCountsPatterns = { "*" };

	private String[] enabledStatsPatterns = { "*" };

	private volatile long shutdownDeadline;

	private final AtomicBoolean shuttingDown = new AtomicBoolean();

	private StringValueResolver embeddedValueResolver;

	private MetricsFactory metricsFactory = new DefaultMetricsFactory();


	public IntegrationMBeanExporter() {
		super();
		// Shouldn't be necessary, but to be on the safe side...
		setAutodetect(false);
		setNamingStrategy(defaultNamingStrategy);
		setAssembler(assembler);
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

	/**
	 * Set the array of simple patterns for component names for which message counts will
	 * be enabled (defaults to '*'). Only patterns that also match
	 * {@link #setComponentNamePatterns(String[]) componentNamePatterns} will be
	 * considered. Enables message counting (`sendCount`, `errorCount`, `receiveCount`)
	 * for those components that support counters (channels, message handlers, etc).
	 * This is the initial setting only, individual components can have counts
	 * enabled/disabled at runtime. May be overridden by an entry in
	 * {@link #setEnabledStatsPatterns(String[]) enabledStatsPatterns} which is additional
	 * functionality over simple counts. If a pattern starts with `!`, counts are disabled
	 * for matches. For components that match multiple patterns, the first pattern wins.
	 * Disabling counts at runtime also disables stats.
	 * @param enabledCountsPatterns the patterns.
	 * @since 4.2
	 */
	public void setEnabledCountsPatterns(String[] enabledCountsPatterns) {
		Assert.notEmpty(enabledCountsPatterns, "enabledCountsPatterns must not be empty");
		this.enabledCountsPatterns = Arrays.copyOf(enabledCountsPatterns, enabledCountsPatterns.length);
	}

	/**
	 * Set the array of simple patterns for component names for which message statistics
	 * will be enabled (response times, rates etc), as well as counts (a positive match
	 * here overrides {@link #setEnabledCountsPatterns(String[]) enabledCountsPatterns},
	 * you can't have statistics without counts). (defaults to '*'). Only patterns that
	 * also match {@link #setComponentNamePatterns(String[]) componentNamePatterns} will
	 * be considered. Enables statistics for those components that support statistics
	 * (channels - when sending, message handlers, etc). This is the initial setting only,
	 * individual components can have stats enabled/disabled at runtime. If a pattern
	 * starts with `!`, stats (and counts) are disabled for matches. Note: this means that
	 * '!foo' here will disable stats and counts for 'foo' even if counts are enabled for
	 * 'foo' in {@link #setEnabledCountsPatterns(String[]) enabledCountsPatterns}. For
	 * components that match multiple patterns, the first pattern wins. Enabling stats at
	 * runtime also enables counts.
	 * @param enabledStatsPatterns the patterns.
	 * @since 4.2
	 */
	public void setEnabledStatsPatterns(String[] enabledStatsPatterns) {
		Assert.notEmpty(enabledStatsPatterns, "componentNamePatterns must not be empty");
		this.enabledStatsPatterns = Arrays.copyOf(enabledStatsPatterns, enabledStatsPatterns.length);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		Assert.notNull(applicationContext, "ApplicationContext may not be null");
		this.applicationContext = applicationContext;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	/**
	 * Set a metrics factory.
	 * @param metricsFactory the factory.
	 * @since 4.2
	 */
	public void setMetricsFactory(MetricsFactory metricsFactory) {
		this.metricsFactory = metricsFactory;
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


	}

	private MessageHandler handlerInAnonymousWrapper(final Object bean) {
		if (bean != null && bean.getClass().isAnonymousClass()) {
			final AtomicReference<MessageHandler> wrapped = new AtomicReference<MessageHandler>();
			ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {

				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
					field.setAccessible(true);
					Object handler = field.get(bean);
					if (handler instanceof MessageHandler) {
						wrapped.set((MessageHandler) handler);
					}
				}
			}, new FieldFilter() {

				@Override
				public boolean matches(Field field) {
					return wrapped.get() == null && field.getName().startsWith("val$");
				}
			});
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
	 * @param beanKey the bean name or human readable version if autogenerated
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
		channelsByName.clear();
		handlersByName.clear();
		sourcesByName.clear();
		for (MessageChannelMetrics monitor : channels) {
			logger.info("Summary on shutdown: " + monitor);
		}
		for (MessageHandlerMetrics monitor : handlers) {
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
			long timeLeft = shutdownDeadline - System.currentTimeMillis();
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
			shuttingDown.set(false);
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

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Channel Count")
	public int getChannelCount() {
		return channelsByName.size();
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageHandler Handler Count")
	public int getHandlerCount() {
		return handlersByName.size();
	}

	@ManagedAttribute
	public String[] getHandlerNames() {
		return handlersByName.keySet().toArray(new String[handlersByName.size()]);
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Active Handler Count")
	public int getActiveHandlerCount() {
		return (int) getActiveHandlerCountLong();
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Active Handler Count")
	public long getActiveHandlerCountLong() {
		int count = 0;
		for (MessageHandlerMetrics monitor : handlers) {
			count += monitor.getActiveCountLong();
		}
		return count;
	}

	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Queued Message Count")
	public int getQueuedMessageCount() {
		int count = 0;
		for (MessageChannelMetrics monitor : channels) {
			if (monitor instanceof QueueChannel) {
				count += ((QueueChannel) monitor).getQueueSize();
			}
		}
		return count;
	}

	@ManagedAttribute
	public String[] getChannelNames() {
		return channelsByName.keySet().toArray(new String[channelsByName.size()]);
	}

	public Statistics getHandlerDuration(String name) {
		if (handlersByName.containsKey(name)) {
			return handlersByName.get(name).getDuration();
		}
		logger.debug("No handler found for (" + name + ")");
		return null;
	}

	public int getSourceMessageCount(String name) {
		return (int) getSourceMessageCountLong(name);
	}

	public long getSourceMessageCountLong(String name) {
		if (sourcesByName.containsKey(name)) {
			return sourcesByName.get(name).getMessageCountLong();
		}
		logger.debug("No source found for (" + name + ")");
		return -1;
	}

	public int getChannelReceiveCount(String name) {
		return (int) getChannelReceiveCountLong(name);
	}

	public long getChannelReceiveCountLong(String name) {
		if (channelsByName.containsKey(name)) {
			if (channelsByName.get(name) instanceof PollableChannelManagement) {
				return ((PollableChannelManagement) channelsByName.get(name)).getReceiveCountLong();
			}
		}
		logger.debug("No channel found for (" + name + ")");
		return -1;
	}

	@ManagedOperation
	public Statistics getChannelSendRate(String name) {
		if (channelsByName.containsKey(name)) {
			return channelsByName.get(name).getSendRate();
		}
		logger.debug("No channel found for (" + name + ")");
		return null;
	}

	public Statistics getChannelErrorRate(String name) {
		if (channelsByName.containsKey(name)) {
			return channelsByName.get(name).getErrorRate();
		}
		logger.debug("No channel found for (" + name + ")");
		return null;
	}

	@SuppressWarnings("unchecked")
	private void registerChannels() {
		for (MessageChannelMetrics monitor : channels) {
			String name = ((NamedComponent) monitor).getComponentName();
			this.allChannelsByName.put(name, monitor);
			if (!matches(this.componentNamePatterns, name)) {
				continue;
			}
			// Only register once...
			if (!channelsByName.containsKey(name)) {
				String beanKey = getChannelBeanKey(name);
				logger.info("Registering MessageChannel " + name);
				if (name != null) {
					channelsByName.put(name, monitor);
				}
				AbstractMessageChannelMetrics metrics = this.metricsFactory.createChannelMetrics(name);
				Assert.state(metrics != null, "'metrics' must not be null");
				Boolean enabled = smartMatch(this.enabledCountsPatterns, name);
				if (enabled != null) {
					monitor.enableCounts(enabled);
				}
				enabled = smartMatch(this.enabledStatsPatterns, name);
				if (enabled != null) {
					monitor.enableStats(enabled);
					metrics.setFullStatsEnabled(enabled);
				}
				if (monitor instanceof ConfigurableMetricsAware) {
					((ConfigurableMetricsAware<AbstractMessageChannelMetrics>) monitor).configureMetrics(metrics);
				}
				registerBeanNameOrInstance(monitor, beanKey);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void registerHandlers() {
		for (MessageHandlerMetrics handler : handlers) {
			MessageHandlerMetrics monitor = enhanceHandlerMonitor(handler);
			String name = monitor.getManagedName();
			this.allHandlersByName.put(name, monitor);
			if (!matches(this.componentNamePatterns, name)) {
				continue;
			}
			// Only register once...
			if (!handlersByName.containsKey(name)) {
				String beanKey = getHandlerBeanKey(monitor);
				if (name != null) {
					handlersByName.put(name, monitor);
				}
				AbstractMessageHandlerMetrics metrics = this.metricsFactory.createHandlerMetrics(name);
				Assert.state(metrics != null, "'metrics' must not be null");
				Boolean enabled = smartMatch(this.enabledCountsPatterns, name);
				if (enabled != null) {
					monitor.enableCounts(enabled);
				}
				enabled = smartMatch(this.enabledStatsPatterns, name);
				if (enabled != null) {
					monitor.enableStats(enabled);
					metrics.setFullStatsEnabled(enabled);
				}
				if (monitor instanceof ConfigurableMetricsAware) {
					((ConfigurableMetricsAware<AbstractMessageHandlerMetrics>) monitor).configureMetrics(metrics);
				}
				registerBeanNameOrInstance(monitor, beanKey);
			}
		}
	}

	private void registerSources() {
		for (MessageSourceMetrics source : sources) {
			MessageSourceMetrics monitor = enhanceSourceMonitor(source);
			String name = monitor.getManagedName();
			this.allSourcesByName.put(name, monitor);
			if (!matches(this.componentNamePatterns, name)) {
				continue;
			}
			// Only register once...
			if (!sourcesByName.containsKey(name)) {
				String beanKey = getSourceBeanKey(monitor);
				if (name != null) {
					sourcesByName.put(name, monitor);
				}
				Boolean enabled = smartMatch(this.enabledCountsPatterns, name);
				if (enabled != null) {
					monitor.enableCounts(enabled);
				}
				registerBeanNameOrInstance(monitor, beanKey);
			}
		}
	}

	private void registerEndpoints() {
		String[] names = this.applicationContext.getBeanNamesForType(AbstractEndpoint.class);
		Set<String> endpointNames = new HashSet<String>();
		for (String name : names) {
			if (!beansByEndpointName.values().contains(name)) {
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
					String unique = name+"#"+count;
					while (endpointNames.contains(unique)) {
						unique = name + "#" + (++count);
					}
					name = unique;
				}
				endpointNames.add(name);
				beanKey = getEndpointBeanKey(endpoint, name, source);
				ObjectName objectName = registerBeanInstance(new ManagedEndpoint(endpoint), beanKey);
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
		Boolean match = smartMatch(patterns, name);
		return match == null ? false : match;
	}

	/**
	 * Simple pattern match against the supplied patterns; also supports negated ('!')
	 * patterns. First match wins (positive or negative).
	 * @param patterns the patterns.
	 * @param name the name to match.
	 * @return null if no match; true for positive match; false for negative match.
	 */
	private Boolean smartMatch(String[] patterns, String name) {
		if (patterns != null) {
			for (String pattern : patterns) {
				boolean reverse = false;
				String patternToUse = pattern;
				if (pattern.startsWith("!")) {
					reverse = true;
					patternToUse = pattern.substring(1);
				}
				else if (pattern.startsWith("\\")) {
					patternToUse = pattern.substring(1);
				}
				if (PatternMatchUtils.simpleMatch(patternToUse, name)) {
					return !reverse;
				}
			}
		}
		return null;
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
		return String.format(domain + ":type=MessageChannel,name=%s" + getStaticNames(), name);
	}

	private String getHandlerBeanKey(MessageHandlerMetrics handler) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(domain + ":type=MessageHandler,name=%s,bean=%s" + getStaticNames(),
				handler.getManagedName(), handler.getManagedType());
	}

	private String getSourceBeanKey(MessageSourceMetrics source) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(domain + ":type=MessageSource,name=%s,bean=%s" + getStaticNames(),
				source.getManagedName(), source.getManagedType());
	}

	private String getEndpointBeanKey(AbstractEndpoint endpoint, String name, String source) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(domain + ":type=ManagedEndpoint,name=%s,bean=%s" + getStaticNames(), name, source);
	}

	private String getStaticNames() {
		if (objectNameStaticProperties.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();

		for (Object key : objectNameStaticProperties.keySet()) {
			builder.append("," + key + "=" + objectNameStaticProperties.get(key));
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
						logger.debug("Could not get handler from bean = " + name);
					}
				}
			}
			Object field = getField(target, "inputChannel");
			if (field != null) {
				if (!anonymousHandlerCounters.containsKey(field)) {
					anonymousHandlerCounters.put(field, new AtomicLong());
				}
				AtomicLong count = anonymousHandlerCounters.get(field);
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
			beansByEndpointName.put(name, endpointName);
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
			try {
				field = extractTarget(getField(endpoint, "source"));
			}
			catch (Exception e) {
				logger.trace("Could not get source from bean = " + beanName);
			}
			if (field == monitor) {
				name = beanName;
				endpointName = beanName;
				break;
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
						logger.debug("Could not get handler from bean = " + name);
					}
				}
			}
			Object field = getField(target, "outputChannel");
			if (field != null) {
				if (!anonymousSourceCounters.containsKey(field)) {
					anonymousSourceCounters.put(field, new AtomicLong());
				}
				AtomicLong count = anonymousSourceCounters.get(field);
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
			if (endpoint instanceof TrackableComponent) {
				result = new LifecycleTrackableMessageSourceMetrics((Lifecycle) endpoint, monitor);
			}
			else {
				result = new LifecycleMessageSourceMetrics((Lifecycle) endpoint, monitor);
			}
		}

		if (name == null) {
			name = monitor.toString();
			source = "handler";
		}

		if (endpointName != null) {
			beansByEndpointName.put(name, endpointName);
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

	private class IntegrationJmxAttributeSource extends AnnotationJmxAttributeSource {

		@Override
		public org.springframework.jmx.export.metadata.ManagedResource getManagedResource(Class<?> beanClass)
				throws InvalidMetadataException {
			IntegrationManagedResource ann =
					AnnotationUtils.getAnnotation(beanClass, IntegrationManagedResource.class);
			if (ann == null) {
				return null;
			}
			org.springframework.jmx.export.metadata.ManagedResource managedResource =
					new org.springframework.jmx.export.metadata.ManagedResource();
			AnnotationBeanUtils.copyPropertiesToBean(ann, managedResource,
					IntegrationMBeanExporter.this.embeddedValueResolver);
			if (!"".equals(ann.value()) && !StringUtils.hasLength(managedResource.getObjectName())) {
				String value = ann.value();
				if (IntegrationMBeanExporter.this.embeddedValueResolver != null) {
					value = IntegrationMBeanExporter.this.embeddedValueResolver.resolveStringValue(value);
				}
				managedResource.setObjectName(value);
			}
			return managedResource;
		}

	}

}

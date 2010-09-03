/*
 * Copyright 2009-2010 the original author or authors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.Lifecycle;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.naming.MetadataNamingStrategy;
import org.springframework.jmx.support.MetricType;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * MBean exporter for Spring Integration components in an existing application.
 * 
 * @author Dave Syer
 * @author Helena Edelson
 */
@ManagedResource
public class IntegrationMBeanExporter extends MBeanExporter implements BeanPostProcessor, BeanFactoryAware,
		BeanClassLoaderAware, SmartLifecycle, ObjectNameLocator {

	private static final Log logger = LogFactory.getLog(IntegrationMBeanExporter.class);

	public static final String DEFAULT_DOMAIN = "spring.application";

	private Set<String> channelKeys = new HashSet<String>();

	private Set<String> handlerKeys = new HashSet<String>();

	private final AnnotationJmxAttributeSource attributeSource = new AnnotationJmxAttributeSource();

	private ListableBeanFactory beanFactory;

	private Map<Object, AtomicLong> anonymousCounters = new HashMap<Object, AtomicLong>();

	private Set<SimpleMessageHandlerMonitor> handlers = new HashSet<SimpleMessageHandlerMonitor>();

	private Set<SimpleMessageChannelMonitor> channels = new HashSet<SimpleMessageChannelMonitor>();

	private Map<String, SimpleMessageChannelMonitor> channelsByName = new HashMap<String, SimpleMessageChannelMonitor>();

	private Map<String, MessageHandlerMonitor> handlersByName = new HashMap<String, MessageHandlerMonitor>();

	private Map<String, String> objectNamesByName = new HashMap<String, String>();

	private ClassLoader beanClassLoader;

	private volatile boolean autoStartup = true;

	private volatile int phase = 0;

	private volatile boolean running;

	private final ReentrantLock lifecycleLock = new ReentrantLock();

	private String domain = DEFAULT_DOMAIN;

	private final Map<String, String> objectNameStaticProperties = new HashMap<String, String>();

	public IntegrationMBeanExporter() {
		super();
		// Shouldn't be necessary, but to be on the safe side...
		setAutodetect(false);
		setNamingStrategy(new MetadataNamingStrategy(attributeSource));
		setAssembler(new MetadataMBeanInfoAssembler(attributeSource));
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
		super.setBeanClassLoader(classLoader);
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
	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		Assert.isTrue(beanFactory instanceof ListableBeanFactory, "A ListableBeanFactory is required.");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof MessageHandler) {
			SimpleMessageHandlerMonitor monitor = new SimpleMessageHandlerMonitor((MessageHandler) bean);
			handlers.add(monitor);
			return monitor;
		}
		if (bean instanceof MessageChannel) {
			SimpleMessageChannelMonitor monitor;
			if (bean instanceof PollableChannel) {
				Object target = extractTarget(bean);
				if (target instanceof QueueChannel) {
					monitor = new QueueChannelMonitor((QueueChannel) target, beanName);
				}
				else {
					monitor = new PollableChannelMonitor(beanName);
				}
			}
			else {
				monitor = new SimpleMessageChannelMonitor(beanName);
			}
			Object advised = applyChannelInterceptor(bean, monitor, beanClassLoader);
			channels.add(monitor);
			return advised;
		}
		return bean;
	}

	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	protected void registerBeans() {
		// Completely disable sup class registration to avoid duplicates
	}

	public final boolean isAutoStartup() {
		return this.autoStartup;
	}

	public final int getPhase() {
		return this.phase;
	}

	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public final void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				this.doStart();
				this.running = true;
				if (logger.isInfoEnabled()) {
					logger.info("started " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public final void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				this.doStop();
				this.running = false;
				if (logger.isInfoEnabled()) {
					logger.info("stopped " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public final void stop(Runnable callback) {
		this.lifecycleLock.lock();
		try {
			this.stop();
			callback.run();
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	protected void doStop() {
	}

	protected void doStart() {
		registerChannels();
		registerHandlers();
		logger.info("Summary on start: " + objectNamesByName);
	}

	@Override
	public void destroy() {
		super.destroy();
		for (MessageChannelMonitor monitor : channels) {
			logger.info("Summary on shutdown: " + monitor);
		}
		for (MessageHandlerMonitor monitor : handlers) {
			logger.info("Summary on shutdown: " + monitor);
		}
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageChannel Channel Count")
	public double getChannelCount() {
		return channelKeys.size();
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "MessageHandler Handler Count")
	public double getHandlerCount() {
		return handlerKeys.size();
	}

	@ManagedAttribute
	public Collection<String> getHandlerNames() {
		return handlersByName.keySet();
	}

	@ManagedAttribute
	public Collection<String> getChannelNames() {
		return channelsByName.keySet();
	}

	@ManagedOperation(description = "Get the JMX object name (as a String) for the specified Spring bean name")
	public String getObjectName(String beanName) {
		return objectNamesByName.get(beanName);
	}

	@ManagedAttribute(description = "Get a map from Spring bean names to JMX object names registered by this exporter")
	public Map<String, String> getObjectNames() {
		return Collections.unmodifiableMap(objectNamesByName);
	}

	public double getHandlerMeanDuration(String name) {
		if (handlersByName.containsKey(name)) {
			return handlersByName.get(name).getMeanDuration();
		}
		logger.debug("No handler found for (" + name + ")");
		return -1;
	}

	public long getChannelSendCount(String name) {
		if (channelsByName.containsKey(name)) {
			return channelsByName.get(name).getSendCount();
		}
		logger.debug("No channel found for (" + name + ")");
		return -1;
	}

	public long getChannelSendErrorCount(String name) {
		if (channelsByName.containsKey(name)) {
			return channelsByName.get(name).getSendErrorCount();
		}
		logger.debug("No channel found for (" + name + ")");
		return -1;
	}

	public long getChannelReceiveCount(String name) {
		if (channelsByName.containsKey(name)) {
			if (channelsByName.get(name) instanceof PollableChannelMonitor) {
				return ((PollableChannelMonitor) channelsByName.get(name)).getReceiveCount();
			}
		}
		logger.debug("No channel found for (" + name + ")");
		return -1;
	}

	public double getChannelSendRate(String name) {
		if (channelsByName.containsKey(name)) {
			return channelsByName.get(name).getSendRate();
		}
		logger.debug("No channel found for (" + name + ")");
		return -1;
	}

	public double getChannelErrorRate(String name) {
		if (channelsByName.containsKey(name)) {
			return channelsByName.get(name).getErrorRate();
		}
		logger.debug("No channel found for (" + name + ")");
		return -1;
	}

	public double getChannelMeanSendDuration(String name) {
		if (channelsByName.containsKey(name)) {
			return channelsByName.get(name).getMeanSendDuration();
		}
		logger.debug("No channel found for (" + name + ")");
		return -1;
	}

	private void registerChannels() {
		for (SimpleMessageChannelMonitor monitor : channels) {
			String name = monitor.getName();
			// Only register once...
			if (!channelsByName.containsKey(name)) {
				String beanKey = getChannelBeanKey(name);
				logger.info("Registering MessageChannel " + name);
				if (name != null) {
					channelsByName.put(name, monitor);
					objectNamesByName.put(name, beanKey);
				}
				registerBeanNameOrInstance(monitor, beanKey);
			}
		}
	}

	private void registerHandlers() {
		for (SimpleMessageHandlerMonitor source : handlers) {
			MessageHandlerMonitor monitor = enhanceMonitor(source);
			String name = monitor.getName();
			// Only register once...
			if (!handlersByName.containsKey(name)) {
				String beanKey = getHandlerBeanKey(monitor);
				if (name != null) {
					handlersByName.put(name, monitor);
					objectNamesByName.put(name, beanKey);
				}
				registerBeanNameOrInstance(monitor, beanKey);
			}
		}
	}

	private Object applyChannelInterceptor(Object bean, SimpleMessageChannelMonitor interceptor,
			ClassLoader beanClassLoader) {
		NameMatchMethodPointcutAdvisor channelsAdvice = new NameMatchMethodPointcutAdvisor(interceptor);
		channelsAdvice.addMethodName("send");
		channelsAdvice.addMethodName("receive");
		return applyAdvice(bean, channelsAdvice, beanClassLoader);
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

	private Object applyAdvice(Object bean, PointcutAdvisor advisor, ClassLoader beanClassLoader) {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (AopUtils.canApply(advisor.getPointcut(), targetClass)) {
			if (bean instanceof Advised) {
				((Advised) bean).addAdvisor(advisor);
				return bean;
			}
			else {
				ProxyFactory proxyFactory = new ProxyFactory(bean);
				proxyFactory.addAdvisor(advisor);
				return proxyFactory.getProxy(beanClassLoader);
			}
		}
		return bean;
	}

	private String getChannelBeanKey(String channel) {
		String name = "" + channel;
		if (name.startsWith("org.springframework.integration")) {
			name = name + ",source=anonymous";
		}
		return String.format(domain + ":type=MessageChannel,name=%s" + getStaticNames(), name);
	}

	private String getHandlerBeanKey(MessageHandlerMonitor handler) {
		// This ordering of keys seems to work with default settings of JConsole
		return String.format(domain + ":type=MessageHandler,name=%s,bean=%s" + getStaticNames(), handler.getName(),
				handler.getSource());
	}

	private String getStaticNames() {
		if (objectNameStaticProperties.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (String key : objectNameStaticProperties.keySet()) {
			builder.append("," + key + "=" + objectNameStaticProperties.get(key));
		}
		return builder.toString();
	}

	private MessageHandlerMonitor enhanceMonitor(SimpleMessageHandlerMonitor monitor) {

		MessageHandlerMonitor result = monitor;

		if (monitor.getName() != null && monitor.getSource() != null) {
			return monitor;
		}

		// Assignment algorithm and bean id, with bean id pulled reflectively out of enclosing endpoint if possible
		String[] names = beanFactory.getBeanNamesForType(AbstractEndpoint.class);

		String name = null;
		String source = "endpoint";
		Object endpoint = null;

		for (String beanName : names) {
			endpoint = beanFactory.getBean(beanName);
			Object field = null;
			try {
				field = getField(endpoint, "handler");
			}
			catch (Exception e) {
				logger.debug("Could not get handler from bean = " + beanName);
			}
			if (field == monitor) {
				name = beanName;
				break;
			}
		}
		if (name != null && endpoint != null && name.startsWith("_org.springframework.integration")) {
			name = name.substring("_org.springframework.integration".length() + 1);
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
				if (!anonymousCounters.containsKey(field)) {
					anonymousCounters.put(field, new AtomicLong());
				}
				AtomicLong count = anonymousCounters.get(field);
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
			result = new LifecycleMessageHandlerMonitor((Lifecycle) endpoint, monitor);
		}

		if (name == null) {
			name = monitor.getMessageHandler().toString();
			source = "handler";
		}

		monitor.setSource(source);
		monitor.setName(name);

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

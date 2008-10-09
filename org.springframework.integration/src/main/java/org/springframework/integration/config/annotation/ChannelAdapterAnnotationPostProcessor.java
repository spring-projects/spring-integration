/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.ChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.config.MessageBusParser;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.PollingConsumerEndpoint;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;
import org.springframework.integration.message.MethodInvokingConsumer;
import org.springframework.integration.message.MethodInvokingSource;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Post-processor for methods annotated with {@link ChannelAdapter @ChannelAdapter}.
 *
 * @author Mark Fisher
 */
public class ChannelAdapterAnnotationPostProcessor implements MethodAnnotationPostProcessor<ChannelAdapter> {

	private final ConfigurableBeanFactory beanFactory;

	private final ChannelRegistry channelRegistry;


	public ChannelAdapterAnnotationPostProcessor(ConfigurableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
		this.channelRegistry = (ChannelRegistry)
				this.beanFactory.getBean(MessageBusParser.MESSAGE_BUS_BEAN_NAME);
	}


	public Object postProcess(Object bean, String beanName, Method method, ChannelAdapter annotation) {
		Assert.notNull(this.beanFactory, "BeanFactory must not be null");
		MessageEndpoint endpoint = null;
		String channelName = annotation.value();
		MessageChannel channel = this.channelRegistry.lookupChannel(channelName);
		if (channel == null) {
			DirectChannel directChannel = new DirectChannel();
			directChannel.setBeanName(channelName);
			this.beanFactory.registerSingleton(channelName, directChannel);
			channel = directChannel;
		}
		Poller pollerAnnotation = AnnotationUtils.findAnnotation(method, Poller.class);
		if (method.getParameterTypes().length == 0 && hasReturnValue(method)) {
			MethodInvokingSource source = new MethodInvokingSource();
			source.setObject(bean);
			source.setMethod(method);
			endpoint = this.createInboundChannelAdapter(source, channel, pollerAnnotation);
		}
		else if (method.getParameterTypes().length > 0 && !hasReturnValue(method)) {
			MethodInvokingConsumer consumer = new MethodInvokingConsumer(bean, method);
			endpoint = this.createOutboundChannelAdapter(consumer, channel, pollerAnnotation);
		}
		else {
			throw new IllegalArgumentException("The @ChannelAdapter can only be applied to methods"
					+ " that accept no arguments but have a return value (inbound) or methods that"
					+ " have no return value but do accept arguments (outbound)");
		}
		if (endpoint != null) {
			String annotationName = ClassUtils.getShortNameAsProperty(annotation.annotationType());
			String endpointName = beanName + "." + method.getName() + "." + annotationName;
			this.beanFactory.registerSingleton(endpointName, endpoint);
		}
		return bean;
	}

	private SourcePollingChannelAdapter createInboundChannelAdapter(MethodInvokingSource source, MessageChannel channel, Poller pollerAnnotation) {
		Assert.notNull(pollerAnnotation, "The @Poller annotation is required (at method-level) "
					+ "when using the @ChannelAdapter annotation with a no-arg method.");
		Trigger trigger = this.createTrigger(pollerAnnotation);
		SourcePollingChannelAdapter adapter = new SourcePollingChannelAdapter();
		adapter.setSource(source);
		adapter.setOutputChannel(channel);
		adapter.setTrigger(trigger);
		if (StringUtils.hasText(pollerAnnotation.transactionManager())) {
			String txManagerRef = pollerAnnotation.transactionManager();
			Assert.isTrue(this.beanFactory.containsBean(txManagerRef),
					"failed to resolve transactionManager reference, no such bean '" + txManagerRef + "'");
			PlatformTransactionManager txManager = (PlatformTransactionManager)
					this.beanFactory.getBean(txManagerRef, PlatformTransactionManager.class);
			adapter.setTransactionManager(txManager);
			Transactional txAnnotation = pollerAnnotation.transactionAttributes();
			adapter.setTransactionDefinition(this.parseTransactionAnnotation(txAnnotation));
		}
		String[] adviceChainArray = pollerAnnotation.adviceChain();
		if (adviceChainArray.length > 0) {
			List<Advice> adviceChain = new ArrayList<Advice>();
			for (String adviceChainString : adviceChainArray) {
				String[] adviceRefs = StringUtils.tokenizeToStringArray(adviceChainString, ",");
				for (String adviceRef : adviceRefs) {
					adviceChain.add((Advice) this.beanFactory.getBean(adviceRef, Advice.class));
				}
			}
			adapter.setAdviceChain(adviceChain);
		}
		return adapter;
	}

	private MessageEndpoint createOutboundChannelAdapter(MethodInvokingConsumer consumer, MessageChannel channel, Poller pollerAnnotation) {
		if (channel instanceof PollableChannel) {
			Trigger trigger = (pollerAnnotation != null)
					? this.createTrigger(pollerAnnotation)
					: new IntervalTrigger(0);
			PollingConsumerEndpoint endpoint = new PollingConsumerEndpoint(consumer, (PollableChannel) channel);
			endpoint.setTrigger(trigger);
			return endpoint;
		}
		if (channel instanceof SubscribableChannel) {
			return new SubscribingConsumerEndpoint(consumer, (SubscribableChannel) channel);
		}
		return null;
	}

	private Trigger createTrigger(Poller pollerAnnotation) {
		IntervalTrigger trigger = new IntervalTrigger(
				pollerAnnotation.interval(), pollerAnnotation.timeUnit());
		trigger.setInitialDelay(pollerAnnotation.initialDelay());
		trigger.setFixedRate(pollerAnnotation.fixedRate());
		return trigger;
	}

	private boolean hasReturnValue(Method method) {
		return !method.getReturnType().equals(void.class);
	}

	@SuppressWarnings("unchecked")
	private TransactionAttribute parseTransactionAnnotation(Transactional annotation) {
		if (annotation == null) {
			return null;
		}
		RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
		rbta.setPropagationBehavior(annotation.propagation().value());
		rbta.setIsolationLevel(annotation.isolation().value());
		rbta.setTimeout(annotation.timeout());
		rbta.setReadOnly(annotation.readOnly());
		ArrayList<RollbackRuleAttribute> rollBackRules = new ArrayList<RollbackRuleAttribute>();
		Class<?>[] rbf = annotation.rollbackFor();
		for (int i = 0; i < rbf.length; ++i) {
			RollbackRuleAttribute rule = new RollbackRuleAttribute(rbf[i]);
			rollBackRules.add(rule);
		}
		String[] rbfc = annotation.rollbackForClassName();
		for (int i = 0; i < rbfc.length; ++i) {
			RollbackRuleAttribute rule = new RollbackRuleAttribute(rbfc[i]);
			rollBackRules.add(rule);
		}
		Class<?>[] nrbf = annotation.noRollbackFor();
		for (int i = 0; i < nrbf.length; ++i) {
			NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(nrbf[i]);
			rollBackRules.add(rule);
		}
		String[] nrbfc = annotation.noRollbackForClassName();
		for (int i = 0; i < nrbfc.length; ++i) {
			NoRollbackRuleAttribute rule = new NoRollbackRuleAttribute(nrbfc[i]);
			rollBackRules.add(rule);
		}
		rbta.getRollbackRules().addAll(rollBackRules);
		return rbta;
	}

}

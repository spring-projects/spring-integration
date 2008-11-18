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

import java.util.ArrayList;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.SpringTransactionAnnotationParser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility methods for working with annotations on Messaging components.
 * 
 * @author Mark Fisher
 */
public abstract class AnnotationConfigUtils {

	public static void configurePollingEndpointWithPollerAnnotation(
			AbstractPollingEndpoint endpoint, Poller pollerAnnotation, BeanFactory beanFactory) {
		Trigger trigger = parseTriggerFromPollerAnnotation(pollerAnnotation);
		endpoint.setTrigger(trigger);
		endpoint.setMaxMessagesPerPoll(pollerAnnotation.maxMessagesPerPoll());
		if (StringUtils.hasText(pollerAnnotation.transactionManager())) {
			String txManagerRef = pollerAnnotation.transactionManager();
			Assert.isTrue(beanFactory.containsBean(txManagerRef),
					"failed to resolve transactionManager reference, no such bean '" + txManagerRef + "'");
			PlatformTransactionManager txManager = (PlatformTransactionManager)
					beanFactory.getBean(txManagerRef, PlatformTransactionManager.class);
			endpoint.setTransactionManager(txManager);
			Transactional txAnnotation = pollerAnnotation.transactionAttributes();
			SpringTransactionAnnotationParser txParser = new SpringTransactionAnnotationParser();
			endpoint.setTransactionDefinition(txParser.parseTransactionAnnotation(txAnnotation));
		}
		if (StringUtils.hasText(pollerAnnotation.taskExecutor())) {
			String taskExecutorRef = pollerAnnotation.taskExecutor();
			Assert.isTrue(beanFactory.containsBean(taskExecutorRef),
					"failed to resolve taskExecutor reference, no such bean '" + taskExecutorRef + "'");
			TaskExecutor taskExecutor = (TaskExecutor) beanFactory.getBean(taskExecutorRef, TaskExecutor.class);
			endpoint.setTaskExecutor(taskExecutor);
		}
		String[] adviceChainArray = pollerAnnotation.adviceChain();
		if (adviceChainArray.length > 0) {
			List<Advice> adviceChain = new ArrayList<Advice>();
			for (String adviceChainString : adviceChainArray) {
				String[] adviceRefs = StringUtils.tokenizeToStringArray(adviceChainString, ",");
				for (String adviceRef : adviceRefs) {
					adviceChain.add((Advice) beanFactory.getBean(adviceRef, Advice.class));
				}
			}
			endpoint.setAdviceChain(adviceChain);
		}
	}

	public static Trigger parseTriggerFromPollerAnnotation(Poller pollerAnnotation) {
		IntervalTrigger trigger = new IntervalTrigger(
				pollerAnnotation.interval(), pollerAnnotation.timeUnit());
		trigger.setInitialDelay(pollerAnnotation.initialDelay());
		trigger.setFixedRate(pollerAnnotation.fixedRate());
		return trigger;
	}

}

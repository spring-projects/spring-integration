/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.routingslip.ExpressionEvaluatingRoutingSlipRouteStrategy;
import org.springframework.integration.routingslip.RoutingSlipRouteStrategy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The {@code RoutingSlip} {@link HeaderValueMessageProcessor} specific implementation.
 * Accepts the {@code routingSlipPath} array, checks each of them against
 * {@link BeanFactory} on the first {@link #processMessage} invocation.
 * Converts those items, which aren't beans in the application context, to the
 * {@link ExpressionEvaluatingRoutingSlipRouteStrategy} and return a {@code singletonMap}
 * with the {@code path} as {@code key} and {@code 0} as initial {@code routingSlipIndex}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Christian Tzolov
 *
 * @since 4.1
 */
public class RoutingSlipHeaderValueMessageProcessor
		extends AbstractHeaderValueMessageProcessor<Map<List<Object>, Integer>>
		implements BeanFactoryAware {

	private final Lock lock = new ReentrantLock();

	private final List<Object> routingSlipPath;

	private volatile Map<List<Object>, Integer> routingSlip;

	private BeanFactory beanFactory;

	public RoutingSlipHeaderValueMessageProcessor(Object... routingSlipPath) {
		Assert.notNull(routingSlipPath, "'routingSlipPath' must not be null");
		Assert.noNullElements(routingSlipPath, "'routingSlipPath' must not contain null elements");
		for (Object entry : routingSlipPath) {
			if (!(entry instanceof String
					|| entry instanceof MessageChannel
					|| entry instanceof RoutingSlipRouteStrategy)) {
				throw new IllegalArgumentException("The RoutingSlip can contain " +
						"only bean names of MessageChannel or RoutingSlipRouteStrategy, " +
						"or MessageChannel and RoutingSlipRouteStrategy instances: " + entry);
			}
		}
		this.routingSlipPath = Arrays.asList(routingSlipPath);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		this.beanFactory = beanFactory; //NOSONAR (inconsistent sync)
	}

	@Override
	public Map<List<Object>, Integer> processMessage(Message<?> message) {
		// use a local variable to avoid the second access to volatile field on the happy path
		Map<List<Object>, Integer> slip = this.routingSlip;
		if (slip == null) {
			this.lock.lock();
			try {
				slip = this.routingSlip;
				if (slip == null) {
					List<Object> slipPath = this.routingSlipPath;
					List<Object> routingSlipValues = new ArrayList<Object>(slipPath.size());
					for (Object path : slipPath) {
						if (path instanceof String) {
							String entry = (String) path;
							if (this.beanFactory.containsBean(entry)) {
								Object bean = this.beanFactory.getBean(entry);
								if (!(bean instanceof MessageChannel
										|| bean instanceof RoutingSlipRouteStrategy)) {
									throw new IllegalArgumentException("The RoutingSlip can contain " +
											"only bean names of MessageChannel or RoutingSlipRouteStrategy: " + bean);
								}
								routingSlipValues.add(entry);
							}
							else {
								ExpressionEvaluatingRoutingSlipRouteStrategy strategy = new
										ExpressionEvaluatingRoutingSlipRouteStrategy(entry);
								strategy.setBeanFactory(this.beanFactory);
								try {
									strategy.afterPropertiesSet();
								}
								catch (Exception e) {
									throw new IllegalStateException(e);
								}
								routingSlipValues.add(strategy);
							}
						}
						else {
							routingSlipValues.add(path);
						}

					}
					slip = Collections.singletonMap(Collections.unmodifiableList(routingSlipValues), 0);
					this.routingSlip = slip;
				}
			}
			finally {
				this.lock.unlock();
			}
		}
		return slip;
	}

}

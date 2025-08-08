/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.scheduling;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An advice that can be added to a poller's advice chain that determines
 * whether a poll should be skipped or not. May be used to temporarily suspend
 * polling when some downstream condition exists in the flow.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 *
 */
public class PollSkipAdvice implements MethodInterceptor {

	private static final Log LOGGER = LogFactory.getLog(PollSkipAdvice.class);

	private final PollSkipStrategy pollSkipStrategy;

	public PollSkipAdvice() {
		this(new DefaultPollSkipStrategy());
	}

	public PollSkipAdvice(PollSkipStrategy strategy) {
		this.pollSkipStrategy = strategy;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if ("call".equals(invocation.getMethod().getName()) && this.pollSkipStrategy.skipPoll()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Skipping poll because "
						+ this.pollSkipStrategy.getClass().getName()
						+ ".skipPoll() returned true");
			}
			return null;
		}
		else {
			return invocation.proceed();
		}
	}

	private static final class DefaultPollSkipStrategy implements PollSkipStrategy {

		DefaultPollSkipStrategy() {
		}

		@Override
		public boolean skipPoll() {
			return false;
		}

	}

}

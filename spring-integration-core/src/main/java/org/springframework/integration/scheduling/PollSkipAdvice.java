/*
 * Copyright 2014-2020 the original author or authors.
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

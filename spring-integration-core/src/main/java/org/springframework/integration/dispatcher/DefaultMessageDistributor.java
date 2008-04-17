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

package org.springframework.integration.dispatcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.Target;
import org.springframework.integration.message.selector.MessageSelectorRejectedException;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link MessageDistributor} interface.
 * 
 * @author Mark Fisher
 */
public class DefaultMessageDistributor implements MessageDistributor {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final List<Target> targets = new CopyOnWriteArrayList<Target>();

	private final DispatcherPolicy dispatcherPolicy;


	public DefaultMessageDistributor(DispatcherPolicy dispatcherPolicy) {
		Assert.notNull(dispatcherPolicy, "'dispatcherPolicy' must not be null");
		this.dispatcherPolicy = dispatcherPolicy;
	}


	public void addTarget(Target target) {
		this.targets.add(target);
	}

	public boolean removeTarget(Target target) {
		return this.targets.remove(target);
	}

	public boolean distribute(Message<?> message) {
		int attempts = 0;
		List<Target> targets = new ArrayList<Target>(this.targets);
		while (attempts < this.dispatcherPolicy.getRejectionLimit()) {
			if (attempts > 0) {
				if (logger.isDebugEnabled()) {
					logger.debug("target(s) rejected message after " + attempts +
							" attempt(s), will try again after 'retryInterval' of " +
							this.dispatcherPolicy.getRetryInterval() + " milliseconds");
				}
				try {
					Thread.sleep(this.dispatcherPolicy.getRetryInterval());
				}
				catch (InterruptedException iex) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
			Iterator<Target> iter = targets.iterator();
			if (!iter.hasNext()) {
				if (logger.isWarnEnabled()) {
					logger.warn("no active targets");
				}
				return false;
			}
			boolean rejected = false;
			while (iter.hasNext()) {
				Target target = iter.next();
				try {
					boolean sent = target.send(message);
					if (!this.dispatcherPolicy.isPublishSubscribe() && sent) {
						return true;
					}
					iter.remove();
				}
				catch (MessageSelectorRejectedException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("selector rejected message, continuing with other targets if available", e);
					}
				}
				catch (MessageHandlerNotRunningException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("target not running, continuing with other targets if available", e);
					}
				}
				catch (MessageHandlerRejectedExecutionException e) {
					rejected = true;
					if (logger.isDebugEnabled()) {
						logger.debug("target is busy, continuing with other targets if available", e);
					}
				}
			}
			if (!rejected) {
				return true;
			}
			attempts++;
		}
		if (this.dispatcherPolicy.getShouldFailOnRejectionLimit()) {
			throw new MessageDeliveryException(message, "Dispatcher reached rejection limit of "
					+ this.dispatcherPolicy.getRejectionLimit()
					+ ". Consider increasing the target's concurrency and/or "
					+ "the dispatcherPolicy's 'rejectionLimit'.");
		}
		return false;
	}

}

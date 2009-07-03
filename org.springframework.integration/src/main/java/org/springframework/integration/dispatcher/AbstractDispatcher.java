/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for {@link MessageDispatcher} implementations.
 * <p>
 * The subclasses implement the actual dispatching strategy, but this base
 * class manages the registration of {@link MessageHandler}s. Although the
 * implemented dispatching strategies may invoke handles in different ways
 * (e.g. round-robin vs. failover), this class does maintain the order of the
 * underlying collection. See the {@link OrderedAwareLinkedHashSet} for more
 * detail.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 */
public abstract class AbstractDispatcher implements MessageDispatcher {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final Set<MessageHandler> handlers = new OrderedAwareLinkedHashSet<MessageHandler>();

	private final Object handlerListMonitor = new Object();


	@SuppressWarnings("unchecked")
	protected List<MessageHandler> getHandlers() {
		return Collections.unmodifiableList(new ArrayList(this.handlers));
	}

	public boolean addHandler(MessageHandler handler) {
		return this.handlers.add(handler);
	}

	public boolean removeHandler(MessageHandler handler) {
		synchronized (this.handlerListMonitor) {
			return this.handlers.remove(handler);
		}
	}

	public String toString() {
		String handlerList = StringUtils.collectionToCommaDelimitedString(this.handlers);
		return this.getClass().getSimpleName() + " with handlers: " + handlerList;
	}

	/**
	 * Convenience method available for subclasses. Returns 'true' unless a
	 * "Selective Consumer" throws a {@link MessageRejectedException}.
	 */
	protected boolean sendMessageToHandler(Message<?> message, MessageHandler handler) {
		Assert.notNull(message, "'message' must not be null.");
		Assert.notNull(handler, "'handler' must not be null.");
		try {
			handler.handleMessage(message);
			return true;
		}
		catch (MessageRejectedException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Handler '" + handler + "' rejected Message, "
						+ "if other handlers are available this dispatcher may try to send to those.", e);
			}
			return false;
		}
	}

}

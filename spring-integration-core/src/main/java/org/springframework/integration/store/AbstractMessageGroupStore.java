/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.store;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * 
 * @since 2.0
 *
 */
public abstract class AbstractMessageGroupStore implements MessageGroupStore, Iterable<MessageGroup> {

	protected final Log logger = LogFactory.getLog(getClass());

	private Collection<MessageGroupCallback> expiryCallbacks = new LinkedHashSet<MessageGroupCallback>();
	
	private volatile boolean timeoutOnIdle;

	/**
	 * 
	 */
	public AbstractMessageGroupStore() {
		super();
	}

	/**
	 * Convenient injection point for expiry callbacks in the message store. Each of the callbacks provided will simply
	 * be registered with the store using {@link #registerMessageGroupExpiryCallback(MessageGroupCallback)}.
	 * 
	 * @param expiryCallbacks the expiry callbacks to add
	 */
	public void setExpiryCallbacks(Collection<MessageGroupCallback> expiryCallbacks) {
		for (MessageGroupCallback callback : expiryCallbacks) {
			registerMessageGroupExpiryCallback(callback);
		}
	}
	
	public boolean isTimeoutOnIdle() {
		return timeoutOnIdle;
	}

	/**
	 * Allows you to override the rule for the timeout calculation. Typical timeout is based from the time
	 * the {@link MessageGroup} was created. If you want the timeout to be based on the time 
	 * the {@link MessageGroup} was idling (e.g., inactive from the last update) invoke this method with 'true'.
	 * Default is 'false'.
	 */
	public void setTimeoutOnIdle(boolean timeoutOnIdle) {
		this.timeoutOnIdle = timeoutOnIdle;
	}

	public void registerMessageGroupExpiryCallback(MessageGroupCallback callback) {
		expiryCallbacks.add(callback);
	}

	public int expireMessageGroups(long timeout) {
		int count = 0;
		long threshold = System.currentTimeMillis() - timeout;
		for (MessageGroup group : this) {

			long timestamp = group.getTimestamp();
			if (this.isTimeoutOnIdle() && group.getLastModified() > 0) {
			    timestamp = group.getLastModified();
			}
			if (timestamp <= threshold) {
				count++;
				expire(group);
			}
		}
		return count;
	}

	@ManagedAttribute
	public int getMessageCountForAllMessageGroups() {
		int count = 0;
		for (MessageGroup group : this) {
			count += group.size();
		}
		return count;
	}

	@ManagedAttribute
	public int getMessageGroupCount() {
		int count = 0;
		for (@SuppressWarnings("unused") MessageGroup group : this) {
			count ++;
		}
		return count;
	}

	private void expire(MessageGroup group) {
	
		RuntimeException exception = null;
	
		for (MessageGroupCallback callback : expiryCallbacks) {
			try {
				callback.execute(this, group);
			} catch (RuntimeException e) {
				if (exception == null) {
					exception = e;
				}
				logger.error("Exception in expiry callback", e);
			}
		}
	
		if (exception != null) {
			throw exception;
		}
	}

}
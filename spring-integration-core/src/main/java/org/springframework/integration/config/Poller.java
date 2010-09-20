/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.config;

import java.util.concurrent.Callable;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.message.ErrorMessage;
/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class Poller implements Runnable {
	public static final int MAX_MESSAGES_UNBOUNDED = -1;
	private volatile long maxMessagesPerPoll = MAX_MESSAGES_UNBOUNDED; 
	private Callable<Boolean> pollingTask;
	/**
	 * @param pollingTask
	 */
	public Poller(Callable<Boolean> pollingTask){
		this.pollingTask = pollingTask;
	}
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		int count = 0;
		while (maxMessagesPerPoll <= 0 || count < maxMessagesPerPoll) {
			try {
				boolean computed = pollingTask.call();
				if (!computed){
					break;
				}
				count++;
			} catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException)e;
				} else {
					throw new MessageHandlingException(new ErrorMessage(e));
				}
			}
		}
	}
	/**
	 * 
	 * @return
	 */
	public long getMaxMessagesPerPoll() {
		return maxMessagesPerPoll;
	}
	/**
	 * Set the maximum number of messages to receive for each poll.
	 * A non-positive value indicates that polling should repeat as long
	 * as non-null messages are being received and successfully sent.
	 * 
	 * <p>The default is unbounded.
	 * 
	 * @see #MAX_MESSAGES_UNBOUNDED
	 */
	public void setMaxMessagesPerPoll(long maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}
}

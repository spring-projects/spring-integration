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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.message.BlockingSource;
import org.springframework.integration.message.BlockingTarget;
import org.springframework.integration.message.MessageExchangeTemplate;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.message.SubscribableSource;
import org.springframework.integration.scheduling.SchedulableTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 */
public class PollingDispatcher implements SchedulableTask, SubscribableSource {

	public final static int MAX_MESSAGES_UNBOUNDED = -1;

	public final static long DEFAULT_RECEIVE_TIMEOUT = -1;


	private final Log logger = LogFactory.getLog(this.getClass());

	private final PollableSource<?> source;

	private final MessageDispatcher dispatcher;

	private final Schedule schedule;

	private final MessageExchangeTemplate messageExchangeTemplate;

	private volatile int maxMessagesPerPoll = MAX_MESSAGES_UNBOUNDED;


	/**
	 * Create a PollingDispatcher for the provided {@link PollableSource}.
	 * It can be scheduled according to the specified {@link Schedule}.
	 */
	public PollingDispatcher(PollableSource<?> source, Schedule schedule) {
		this(source, schedule, null, null);
	}

	public PollingDispatcher(PollableSource<?> source, Schedule schedule, MessageDispatcher dispatcher) {
		this(source, schedule, dispatcher, null);
	}

	public PollingDispatcher(PollableSource<?> source, Schedule schedule, MessageDispatcher dispatcher, MessageExchangeTemplate messageExchangeTemplate) {
		Assert.notNull(source, "source must not be null");
		this.source = source;
		this.schedule = schedule;
		this.dispatcher = (dispatcher != null)
				? dispatcher : new SimpleDispatcher();
		this.messageExchangeTemplate = (messageExchangeTemplate != null)
				? messageExchangeTemplate : createDefaultTemplate();
	}


	/**
	 * Specify the timeout to use when receiving from the source (in milliseconds).
	 * Note that this value will only be applicable if the source is an instance
	 * of {@link BlockingSource}.
	 * <p/>
	 * A negative value indicates that receive calls should block indefinitely,
	 * and that is the default behavior.
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.messageExchangeTemplate.setReceiveTimeout(receiveTimeout);
	}

	/**
	 * Specify the timeout to use when sending to a target (in milliseconds).
	 * Note that this value will only be applicable if the target is an instance
	 * of {@link BlockingTarget}.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.dispatcher.setTimeout(sendTimeout);
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
	public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
	}

	public boolean subscribe(MessageTarget target) {
		return this.dispatcher.subscribe(target);
	}

	public boolean unsubscribe(MessageTarget target) {
		return this.dispatcher.unsubscribe(target);
	}

	public Schedule getSchedule() {
        return this.schedule;
    }

	public void run() {
		int count = 0;
		while (this.maxMessagesPerPoll < 0 || count < this.maxMessagesPerPoll) {
			if (!this.messageExchangeTemplate.receiveAndForward(this.source, this.dispatcher)) {
				break;
			}
			count++;
		}
		if (this.logger.isTraceEnabled()) {
			this.logger.trace("poller for source '" + this.source + "' sent " + count
					+ " messages to target '" + this.dispatcher + "'");
		}
		return;
	}

	public String toString() {
		return this.getClass().getSimpleName() + " [source = " + this.source
				+ ", dispatcher = [" + this.dispatcher + "]";
	}

	private MessageExchangeTemplate createDefaultTemplate() {
		MessageExchangeTemplate template = new MessageExchangeTemplate();
		template.setReceiveTimeout(DEFAULT_RECEIVE_TIMEOUT);
		template.setSendTimeout(-1);
		return template;
	}

}

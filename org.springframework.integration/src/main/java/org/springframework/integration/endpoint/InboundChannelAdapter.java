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

package org.springframework.integration.endpoint;

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.MethodInvokingSource;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.scheduling.TaskScheduler;

/**
 * A Channel Adapter implementation for connecting a
 * {@link org.springframework.integration.message.MessageSource}
 * to a {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class InboundChannelAdapter extends AbstractEndpoint implements Lifecycle {

	private volatile PollableSource<?> source;

	private volatile MessageChannel channel;

	private volatile Schedule schedule;

	private volatile SourcePoller poller;

	private volatile int maxMessagesPerPoll = -1;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public void setSource(PollableSource<?> source) {
		this.source = source;
	}

	public void setChannel(MessageChannel channel) {
		this.channel = channel;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public void setMaxMessagesPerPoll(int maxMessagesPerPoll) {
		this.maxMessagesPerPoll = maxMessagesPerPoll;
		if (this.poller != null) {
			this.poller.setMaxMessagesPerPoll(maxMessagesPerPoll);
		}
	}

	public final boolean isRunning() {
		return this.running;
	}

	public final void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.running) {
				return;
			}
			this.poller = new SourcePoller(source, channel, schedule);
			if (maxMessagesPerPoll < 0 && source instanceof MethodInvokingSource) {
				// the default is 1 since a MethodInvokingSource might return a non-null value
				// every time it is invoked, thus producing an infinite number of messages per poll
				maxMessagesPerPoll = 1;
			}
			this.configureTransactionSettingsForPoller(this.poller);
			this.poller.setMaxMessagesPerPoll(maxMessagesPerPoll);
			TaskScheduler taskScheduler = this.getTaskScheduler();
			if (taskScheduler != null) {
				taskScheduler.schedule(this.poller);
			}
		}
	}

	public final void stop() {
		synchronized (this.lifecycleMonitor) {
			if (!this.running) {
				return;
			}
			TaskScheduler taskScheduler = this.getTaskScheduler();
			if (taskScheduler != null) {
				taskScheduler.cancel(this.poller, true);
			}
		}
	}

}

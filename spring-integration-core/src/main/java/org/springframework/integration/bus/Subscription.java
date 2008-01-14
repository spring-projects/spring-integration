/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.bus;

import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.scheduling.Schedule;

/**
 * Configuration metadata for activating a subscription.
 * 
 * @author Mark Fisher
 */
public class Subscription {

	private String channel;

	private String handler;

	private Schedule schedule;

	private ConcurrencyPolicy concurrencyPolicy;


	public String getChannel() {
		return this.channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getHandler() {
		return this.handler;
	}

	public void setHandler(String handler) {
		this.handler = handler;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public ConcurrencyPolicy getConcurrencyPolicy() {
		return this.concurrencyPolicy;
	}

	public void setConcurrencyPolicy(ConcurrencyPolicy concurrencyPolicy) {
		this.concurrencyPolicy = concurrencyPolicy;
	}

}

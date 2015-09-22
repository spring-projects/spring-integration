/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.aop;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.util.DynamicPeriodicTrigger;
import org.springframework.messaging.Message;

/**
 * A simple advice that polls at one rate when messages exist and another when
 * there are no messages.
 *
 * @author Gary Russell
 * @since 4.2
 * @see DynamicPeriodicTrigger
 */
public class SimpleActiveIdleMessageSourceAdvice extends AbstractMessageSourceAdvice {

	private final DynamicPeriodicTrigger trigger;

	private volatile long idlePollPeriod;

	private volatile long activePollPeriod;


	public SimpleActiveIdleMessageSourceAdvice(DynamicPeriodicTrigger trigger) {
		this.trigger = trigger;
		this.idlePollPeriod = trigger.getPeriod();
		this.activePollPeriod = trigger.getPeriod();
	}

	/**
	 * Set the poll period when messages are not returned. Defaults to the
	 * trigger's period.
	 * @param idlePollPeriod the period in milliseconds.
	 */
	public void setIdlePollPeriod(long idlePollPeriod) {
		this.idlePollPeriod = idlePollPeriod;
	}

	/**
	 * Set the poll period when messages are returned. Defaults to the
	 * trigger's period.
	 * @param activePollPeriod the period in milliseconds.
	 */
	public void setActivePollPeriod(long activePollPeriod) {
		this.activePollPeriod = activePollPeriod;
	}

	@Override
	public boolean beforeReceive(MessageSource<?> source) {
		return true;
	}

	@Override
	public Message<?> afterReceive(Message<?> result, MessageSource<?> source) {
		if (result == null) {
			this.trigger.setPeriod(this.idlePollPeriod);
		}
		else {
			this.trigger.setPeriod(this.activePollPeriod);
		}
		return result;
	}

}

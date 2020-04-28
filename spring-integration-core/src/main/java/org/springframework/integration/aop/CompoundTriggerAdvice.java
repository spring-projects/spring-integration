/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.aop;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.util.CompoundTrigger;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;

/**
 * An {@link AbstractMessageSourceAdvice} that uses a {@link CompoundTrigger} to adjust
 * the poller - when a message is present, the compound trigger's primary trigger is
 * used to determine the next poll. When no message is present, the override trigger is
 * used.
 * <p>
 * The poller advised by this class must be configured to use the same
 * {@link CompoundTrigger} instance and must <b>not</b> use a task executor.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
@SuppressWarnings("deprecation")
public class CompoundTriggerAdvice
		extends AbstractMessageSourceAdvice
		implements ReceiveMessageAdvice {

	private final CompoundTrigger compoundTrigger;

	private final Trigger override;

	public CompoundTriggerAdvice(CompoundTrigger compoundTrigger, Trigger overrideTrigger) {
		Assert.notNull(compoundTrigger, "'compoundTrigger' cannot be null");
		this.compoundTrigger = compoundTrigger;
		this.override = overrideTrigger;
	}

	/**
	 * @param result the received message.
	 * @param source the message source.
	 * @return the message or null
	 * @deprecated since 5.3 in favor of {@link #afterReceive(Message, Object)}
	 */
	@Override
	@Deprecated
	public Message<?> afterReceive(Message<?> result, MessageSource<?> source) {
		return afterReceive(result, (Object) source);
	}

	@Override
	@Nullable
	public Message<?> afterReceive(@Nullable Message<?> result, Object source) {
		if (result == null) {
			this.compoundTrigger.setOverride(this.override);
		}
		else {
			this.compoundTrigger.setOverride(null);
		}
		return result;
	}

}

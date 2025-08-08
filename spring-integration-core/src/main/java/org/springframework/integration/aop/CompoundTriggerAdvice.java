/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.aop;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.util.CompoundTrigger;
import org.springframework.messaging.Message;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;

/**
 * A {@link MessageSourceMutator} that uses a {@link CompoundTrigger} to adjust
 * the poller - when a message is present, the compound trigger's primary trigger is
 * used to determine the next poll. When no message is present, the override trigger is
 * used.
 * <p>
 * The poller advised by this class must be configured to use the same
 * {@link CompoundTrigger} instance and must <b>not</b> use a task executor.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class CompoundTriggerAdvice
		implements MessageSourceMutator, ReceiveMessageAdvice {

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
	 */
	@Override
	public Message<?> afterReceive(Message<?> result, MessageSource<?> source) {
		if (result == null) {
			this.compoundTrigger.setOverride(this.override);
		}
		else {
			this.compoundTrigger.setOverride(null);
		}
		return result;
	}

}

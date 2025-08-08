/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.util;

import java.time.Instant;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.util.Assert;

/**
 * A {@link Trigger} that delegates the {@link #nextExecutionTime(TriggerContext)}
 * to one of two Triggers. If the {@link #setOverride(Trigger) override} trigger is
 * {@code null}, the primary trigger is invoked; otherwise the override trigger is
 * invoked.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public class CompoundTrigger implements Trigger {

	private volatile Trigger primary;

	private volatile Trigger override;

	/**
	 * Construct a compound trigger with the supplied primary trigger.
	 * @param primary the primary trigger.
	 */
	public CompoundTrigger(Trigger primary) {
		setPrimary(primary);
	}

	/**
	 * Set the primary trigger.
	 * @param primary the trigger.
	 */
	public final void setPrimary(Trigger primary) {
		Assert.notNull(primary, "'primary' cannot be null");
		this.primary = primary;
	}

	/**
	 * Set the override trigger; set to null to revert to using the
	 * primary trigger.
	 * @param override the override trigger, or null.
	 */
	public void setOverride(Trigger override) {
		this.override = override;
	}

	@Override
	public Instant nextExecution(TriggerContext triggerContext) {
		if (this.override != null) {
			return this.override.nextExecution(triggerContext);
		}
		else {
			return this.primary.nextExecution(triggerContext);
		}
	}

}

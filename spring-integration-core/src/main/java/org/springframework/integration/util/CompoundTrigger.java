/*
 * Copyright 2015-2022 the original author or authors.
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

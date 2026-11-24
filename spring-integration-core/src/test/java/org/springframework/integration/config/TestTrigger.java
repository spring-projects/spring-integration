/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import java.time.Instant;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class TestTrigger implements Trigger {

	@Override
	public Instant nextExecution(TriggerContext triggerContext) {
		throw new UnsupportedOperationException();
	}

}

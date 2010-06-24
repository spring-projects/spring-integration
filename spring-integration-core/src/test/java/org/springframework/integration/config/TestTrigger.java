package org.springframework.integration.config;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

import java.util.Date;

/**
 * @author Marius Bogoevici
 */
public class TestTrigger implements Trigger {

    public Date nextExecutionTime(TriggerContext triggerContext) {
        throw new UnsupportedOperationException();
    }

}

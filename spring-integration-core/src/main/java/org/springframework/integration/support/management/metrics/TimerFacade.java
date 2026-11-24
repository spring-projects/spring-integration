/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support.management.metrics;

import java.util.concurrent.TimeUnit;

/**
 * @author Gary Russell
 * @since 5.0.4
 *
 */
public interface TimerFacade extends MeterFacade {

	void record(long time, TimeUnit unit);

}

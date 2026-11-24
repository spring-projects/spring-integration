/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

/**
 * @author Gary Russell
 * @since 5.2
 *
 */
public interface SendTimersAware {

	void sendTimers(Supplier<SendTimers> timers);

}

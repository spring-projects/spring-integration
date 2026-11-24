/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

/**
 * Success and failure timer stats.
 *
 * @param successes the success stats
 * @param failures the failed stats
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
public record SendTimers(TimerStats successes, TimerStats failures) {

}

/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

/**
 * Counters for components that maintain receive counters.
 *
 * @param successes the number of successful {@code receives}.
 * @param  failures the number of failed {@code receives}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
public record ReceiveCounters(long successes, long failures) {

}

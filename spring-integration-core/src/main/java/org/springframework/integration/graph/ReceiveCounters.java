/*
 * Copyright 2019-present the original author or authors.
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

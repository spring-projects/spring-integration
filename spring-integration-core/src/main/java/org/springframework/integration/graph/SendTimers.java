/*
 * Copyright 2019-present the original author or authors.
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

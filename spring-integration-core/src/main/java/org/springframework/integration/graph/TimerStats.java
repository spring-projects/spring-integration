/*
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Statistics captured from a timer meter.
 *
 * @param count the number of times the timer was invoked
 * @param mean the mean of the timer
 * @param max the maximum of the timer
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.2
 *
 */
public record TimerStats(long count, double mean, double max) {

}

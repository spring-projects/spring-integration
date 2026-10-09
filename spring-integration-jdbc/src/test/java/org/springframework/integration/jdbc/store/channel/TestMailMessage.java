/*
 * Copyright 2025-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

/**
 * Test payload class for Jackson JSON serialization tests.
 *
 * @author Yoobin Yoon
 */
public record TestMailMessage(
		String subject,
		String body,
		String to
) {

}

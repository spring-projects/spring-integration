/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

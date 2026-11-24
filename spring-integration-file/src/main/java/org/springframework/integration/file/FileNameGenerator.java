/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file;

import org.springframework.messaging.Message;

/**
 * Strategy interface for generating a file name from a message.
 *
 * @author Mark Fisher
 */
@FunctionalInterface
public interface FileNameGenerator {

	String generateFileName(Message<?> message);

}

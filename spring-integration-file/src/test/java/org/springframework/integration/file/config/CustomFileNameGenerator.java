/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.config;

import java.util.Date;

import org.springframework.integration.file.FileNameGenerator;
import org.springframework.messaging.Message;

/**
 * @author Marius Bogoevici
 */
public class CustomFileNameGenerator implements FileNameGenerator {

	public String generateFileName(Message<?> message) {
		return "file" + new Date().getTime();
	}

}

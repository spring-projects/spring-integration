/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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

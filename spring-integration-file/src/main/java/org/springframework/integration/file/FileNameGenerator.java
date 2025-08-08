/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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

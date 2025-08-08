/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting;

import java.util.Map;

import org.springframework.messaging.Message;

/**
 * Strategy interface to provide a {@link Map} of variables to the script execution context.
 * Variables may be extracted from the {@link Message} argument.
 *
 * @author Oleg Zhurakousky
 * @since 2.0.2
 */
@FunctionalInterface
public interface ScriptVariableGenerator {

	Map<String, Object> generateScriptVariables(Message<?> message);

}

/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml;

import java.util.Collections;
import java.util.List;

/**
 * The validation exception which aggregate all the XML validation errors.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AggregatedXmlMessageValidationException extends RuntimeException {

	private final List<Throwable> exceptions;

	public AggregatedXmlMessageValidationException(List<Throwable> exceptions) {
		this.exceptions = (exceptions != null) ? exceptions : Collections.emptyList();
	}

	@Override
	public String getMessage() {
		StringBuilder message = new StringBuilder("Multiple causes:\n");
		for (Throwable exception : this.exceptions) {
			message.append("    ")
					.append(exception.getMessage())
					.append("\n");
		}
		return message.toString();
	}

	public List<Throwable> getExceptions() {
		return Collections.unmodifiableList(this.exceptions);
	}

}

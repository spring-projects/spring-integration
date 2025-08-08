/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.support.parametersource;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public interface ParameterSourceFactory {

	/**
	 * Return a new {@link ParameterSource}.
	 * @param input The raw message or query result to be transformed into a {@link ParameterSource}.
	 * @return The parameter source.
	 */
	ParameterSource createParameterSource(Object input);

}

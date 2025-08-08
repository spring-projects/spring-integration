/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.springframework.integration.transformer.StreamTransformer;

/**
 * Parser for {@code <stream-transformer/>} element.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class StreamTransformerParser extends ObjectToStringTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return StreamTransformer.class.getName();
	}

}

/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.springframework.integration.file.transformer.FileToByteArrayTransformer;

/**
 * Parser for the &lt;file-to-bytes-transformer&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class FileToByteArrayTransformerParser extends AbstractFilePayloadTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return FileToByteArrayTransformer.class.getName();
	}

}

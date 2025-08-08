/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.transformer;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alex Peters
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class FileToByteArrayTransformerTests extends
		AbstractFilePayloadTransformerTests<FileToByteArrayTransformer> {

	@Before
	public void setUp() {
		transformer = new FileToByteArrayTransformer();
	}

	@Test
	public void transform_withFilePayload_convertedToByteArray() throws Exception {
		Message<?> result = transformer.transform(message);
		assertThat(result).isNotNull();

		assertThat(result.getPayload())
				.isInstanceOf(byte[].class)
				.isEqualTo(SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING));
	}

}

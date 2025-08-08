/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class PayloadTypeConvertingTransformerTests {

	/**
	 * Test method for
	 * {@link org.springframework.integration.transformer.PayloadTypeConvertingTransformer#transformPayload(java.lang.Object)}
	 * .
	 */
	@Test
	public void testTransformPayloadObject() throws Exception {
		PayloadTypeConvertingTransformer<String, String> tx = new PayloadTypeConvertingTransformer<String, String>();
		tx.setConverter(source -> source.toUpperCase());
		String in = "abcd";
		String out = tx.transformPayload(in);
		assertThat(out).isEqualTo("ABCD");
	}

}



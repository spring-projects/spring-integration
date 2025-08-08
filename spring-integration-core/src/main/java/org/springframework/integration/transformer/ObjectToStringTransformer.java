/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.io.UnsupportedEncodingException;

import org.springframework.util.Assert;

/**
 * A simple transformer that creates an outbound payload by invoking the
 * inbound payload Object's <code>toString()</code> method. Unless the
 * payload is a <code>byte[]</code> or <code>char[]</code>. If the payload
 * is a byte[], it will be transformed to a String containing the
 * array's contents, using the {@link #charset}
 * which, by default, is "UTF-8". If the payload is a char[], it will be
 * transformed to a String object with the array's contents.
 *
 * @author Mark Fisher
 * @author Andrew Cowlin
 * @author Gary Russell
 * @since 1.0.1
 */
public class ObjectToStringTransformer extends AbstractPayloadTransformer<Object, String> {

	private final String charset;

	public ObjectToStringTransformer() {
		this.charset = "UTF-8";
	}

	public ObjectToStringTransformer(String charset) {
		Assert.notNull(charset, "'charset' cannot be null");
		this.charset = charset;
	}

	@Override
	public String getComponentType() {
		return "object-to-string-transformer";
	}

	@Override
	protected String transformPayload(Object payload) {
		if (payload instanceof byte[]) {
			try {
				return new String((byte[]) payload, this.charset);
			}
			catch (UnsupportedEncodingException e) {
				throw new IllegalStateException(e);
			}
		}
		else if (payload instanceof char[]) {
			return new String((char[]) payload);
		}
		else {
			return payload.toString();
		}
	}

}

/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http.config;

import java.io.IOException;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class StubHttpMessageConverter extends AbstractHttpMessageConverter<String> {

	@Override
	protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		return null;
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		return false;
	}

	@Override
	protected void writeInternal(String t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
	}

}

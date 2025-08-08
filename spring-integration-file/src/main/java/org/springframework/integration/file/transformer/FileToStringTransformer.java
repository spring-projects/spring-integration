/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.transformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A payload transformer that copies a File's contents to a String.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class FileToStringTransformer extends AbstractFilePayloadTransformer<String> {

	private volatile Charset charset = Charset.defaultCharset();

	/**
	 * Set the charset name to use when copying the File to a String.
	 *
	 * @param charset The charset.
	 */
	public void setCharset(String charset) {
		Assert.notNull(charset, "charset must not be null");
		Assert.isTrue(Charset.isSupported(charset), "Charset '" + charset + "' is not supported.");
		this.charset = Charset.forName(charset);
	}

	@Override
	protected final String transformFile(File file) throws IOException {
		Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), this.charset));
		return FileCopyUtils.copyToString(reader);
	}

}

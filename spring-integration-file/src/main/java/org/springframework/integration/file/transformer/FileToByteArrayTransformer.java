/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.transformer;

import java.io.File;
import java.io.IOException;

import org.springframework.util.FileCopyUtils;

/**
 * A payload transformer that copies a File's contents to a byte array.
 *
 * @author Mark Fisher
 */
public class FileToByteArrayTransformer extends AbstractFilePayloadTransformer<byte[]> {

	@Override
	protected final byte[] transformFile(File file) throws IOException {
		return FileCopyUtils.copyToByteArray(file);
	}

}

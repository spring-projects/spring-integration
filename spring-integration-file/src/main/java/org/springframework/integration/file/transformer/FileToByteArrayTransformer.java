/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

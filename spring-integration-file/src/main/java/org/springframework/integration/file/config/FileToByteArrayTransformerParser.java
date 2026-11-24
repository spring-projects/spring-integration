/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

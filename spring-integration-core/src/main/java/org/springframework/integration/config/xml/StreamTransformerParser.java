/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

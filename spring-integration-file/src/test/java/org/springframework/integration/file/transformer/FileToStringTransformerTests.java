/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.transformer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alex Peters
 * @author Artem Bilan
 */
public class FileToStringTransformerTests extends
		AbstractFilePayloadTransformerTests<FileToStringTransformer> {

	@BeforeEach
	public void setUp() {
		this.transformer = new FileToStringTransformer();
		this.transformer.setCharset(DEFAULT_ENCODING);
		this.transformer.setBeanFactory(TEST_INTEGRATION_CONTEXT);
	}

	@Test
	public void transform_withFilePayload_convertedToString() {
		Message<?> result = transformer.transform(message);
		assertThat(result).isNotNull();
		assertThat(result.getPayload())
				.isInstanceOf(String.class)
				.isEqualTo(SAMPLE_CONTENT);
	}

	@Test
	public void transform_withWrongEncoding_notMatching() {
		transformer.setCharset("ISO-8859-1");
		Message<?> result = transformer.transform(message);
		assertThat(result).isNotNull();
		assertThat(result.getPayload())
				.isInstanceOf(String.class)
				.isNotEqualTo(SAMPLE_CONTENT);
	}

}

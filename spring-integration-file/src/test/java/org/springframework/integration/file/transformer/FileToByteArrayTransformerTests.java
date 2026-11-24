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
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class FileToByteArrayTransformerTests extends
		AbstractFilePayloadTransformerTests<FileToByteArrayTransformer> {

	@BeforeEach
	public void setUp() {
		this.transformer = new FileToByteArrayTransformer();
		this.transformer.setBeanFactory(TEST_INTEGRATION_CONTEXT);
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

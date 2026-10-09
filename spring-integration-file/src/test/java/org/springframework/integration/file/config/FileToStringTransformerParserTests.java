/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class FileToStringTransformerParserTests {

	@Autowired
	@Qualifier("transformer")
	PollingConsumer endpoint;

	@Test
	public void checkDeleteFilesValue() {
		assertThat(TestUtils.getPropertyValue(this.endpoint, "handler.transformer.deleteFiles", Boolean.class)).isTrue();
	}

}

/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.file.transformer.FileToStringTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FileToStringTransformerParserTests {

	@Autowired
	@Qualifier("transformer")
	PollingConsumer endpoint;

	@Test
	public void checkDeleteFilesValue() {
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(endpoint);
		MessageTransformingHandler handler = (MessageTransformingHandler)
				endpointAccessor.getPropertyValue("handler");
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		FileToStringTransformer transformer = (FileToStringTransformer)
				handlerAccessor.getPropertyValue("transformer");
		DirectFieldAccessor transformerAccessor = new DirectFieldAccessor(transformer);
		assertThat(transformerAccessor.getPropertyValue("deleteFiles")).isEqualTo(Boolean.TRUE);
	}

}

/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

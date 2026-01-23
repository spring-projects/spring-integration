/*
 * Copyright 2002-present the original author or authors.
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

import java.io.File;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 1.0.3
 */
@SpringJUnitConfig
@DirtiesContext
public class FileOutboundAdaptersWithClasspathInPropertiesTests {

	@Autowired
	@Qualifier("adapter")
	private EventDrivenConsumer adapter;

	@Autowired
	@Qualifier("gateway")
	private EventDrivenConsumer gateway;

	@Test
	public void outboundChannelAdapter() throws Exception {
		MessageHandler handler = adapter.getHandler();
		File expected = new ClassPathResource("").getFile();

		var destinationDirectoryExpression =
				TestUtils.<Expression>getPropertyValue(handler, "destinationDirectoryExpression");
		File actual = new File(destinationDirectoryExpression.getExpressionString());

		assertThat(actual).as("'destinationDirectory' should be set").isEqualTo(expected);
	}

	@Test
	public void outboundGateway() throws Exception {
		MessageHandler handler = gateway.getHandler();
		File expected = new ClassPathResource("").getFile();

		var destinationDirectoryExpression =
				TestUtils.<Expression>getPropertyValue(handler, "destinationDirectoryExpression");
		File actual = new File(destinationDirectoryExpression.getExpressionString());

		assertThat(actual).as("'destinationDirectory' should be set").isEqualTo(expected);
	}

}

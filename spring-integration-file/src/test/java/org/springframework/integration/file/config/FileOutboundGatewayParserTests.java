/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileOutboundGatewayParserTests {

	@Autowired
	private EventDrivenConsumer ordered;

	@Autowired
	private EventDrivenConsumer gatewayWithDirectoryExpression;

	@Test
	public void checkOrderedGateway() throws Exception {

		DirectFieldAccessor gatewayAccessor = new DirectFieldAccessor(ordered);
		FileWritingMessageHandler handler = (FileWritingMessageHandler)
				gatewayAccessor.getPropertyValue("handler");
		assertEquals(Boolean.FALSE, gatewayAccessor.getPropertyValue("autoStartup"));
		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(handler);
		assertEquals(777, handlerAccessor.getPropertyValue("order"));
		DefaultFileNameGenerator fileNameGenerator = (DefaultFileNameGenerator) handlerAccessor.getPropertyValue("fileNameGenerator");
		assertNotNull(fileNameGenerator);
		String expression = (String) TestUtils.getPropertyValue(fileNameGenerator, "expression");
		assertNotNull(expression);
		assertEquals("'foo.txt'", expression);

		Long sendTimeout = TestUtils.getPropertyValue(handler, "messagingTemplate.sendTimeout", Long.class);
		assertEquals(Long.valueOf(777), sendTimeout);

	}

	@Test
	public void testOutboundGatewayWithDirectoryExpression() throws Exception {
		FileWritingMessageHandler handler = TestUtils.getPropertyValue(gatewayWithDirectoryExpression, "handler", FileWritingMessageHandler.class);
		assertEquals("'build/foo'", TestUtils.getPropertyValue(handler, "destinationDirectoryExpression", Expression.class).getExpressionString());
	}

}

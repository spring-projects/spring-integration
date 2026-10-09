/*
 * Copyright 2002-present the original author or authors.
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
				TestUtils.getPropertyValue(handler, "destinationDirectoryExpression", Expression.class);
		File actual = new File(destinationDirectoryExpression.getExpressionString());

		assertThat(actual).as("'destinationDirectory' should be set").isEqualTo(expected);
	}

	@Test
	public void outboundGateway() throws Exception {
		MessageHandler handler = gateway.getHandler();
		File expected = new ClassPathResource("").getFile();

		var destinationDirectoryExpression =
				TestUtils.getPropertyValue(handler, "destinationDirectoryExpression", Expression.class);
		File actual = new File(destinationDirectoryExpression.getExpressionString());

		assertThat(actual).as("'destinationDirectory' should be set").isEqualTo(expected);
	}

}

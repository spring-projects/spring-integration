/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gunnar Hillert
 *
 * @since 1.0.3
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileOutboundAdaptersWithClasspathInPropertiesTests {

	@Autowired
	@Qualifier("adapter")
	private EventDrivenConsumer adapter;

	@Autowired
	@Qualifier("gateway")
	private EventDrivenConsumer gateway;

	@Test
	public void outboundChannelAdapter() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(adapter).getPropertyValue("handler"));
		File expected = new ClassPathResource("").getFile();

		Expression destinationDirectoryExpression = (Expression) accessor.getPropertyValue("destinationDirectoryExpression");
		File actual = new File(destinationDirectoryExpression.getExpressionString());

		assertThat(actual).as("'destinationDirectory' should be set").isEqualTo(expected);
	}

	@Test
	public void outboundGateway() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(gateway).getPropertyValue("handler"));
		File expected = new ClassPathResource("").getFile();

		Expression destinationDirectoryExpression = (Expression) accessor.getPropertyValue("destinationDirectoryExpression");
		File actual = new File(destinationDirectoryExpression.getExpressionString());

		assertThat(actual).as("'destinationDirectory' should be set").isEqualTo(expected);
	}

}

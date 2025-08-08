/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.ignore;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.0
 */
@SpringJUnitConfig
@Disabled
public class ConsoleChatTests {

	public static void main(String[] args) {
		new ClassPathXmlApplicationContext("ConsoleChatTests-context.xml", ConsoleChatTests.class).close();
	}

	@Test
	public void run() throws Exception {
		Thread.sleep(10 * 1000 * 1000);
	}

}

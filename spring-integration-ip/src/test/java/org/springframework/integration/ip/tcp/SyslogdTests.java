/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class SyslogdTests {

	private SyslogdTests() {
		super();
	}

	public static void main(String[] args) throws Exception {
		AbstractApplicationContext ctx = new ClassPathXmlApplicationContext("SyslogdTests-context.xml", SyslogdTests.class);
		// System . out . println("Hit enter to terminate");
		System.in.read();
		ctx.close();
	}

}

/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration_.mbeanexporterhelper;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 *
 */
public class INT_2626Tests {

	@Test // This context failed to load before the INT-2626 fix was applied
	public void testInt2626() {
		new ClassPathXmlApplicationContext("INT-2626-config.xml", this.getClass()).close();
	}

}

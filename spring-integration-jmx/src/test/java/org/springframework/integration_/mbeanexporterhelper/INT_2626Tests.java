package org.springframework.integration_.mbeanexporterhelper;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class INT_2626Tests {

	@Test
	public void testInt2626(){
		new ClassPathXmlApplicationContext("INT-2626-config.xml", this.getClass());
	}
}

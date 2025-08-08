/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Jim Moore
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ConstructorAutowireTests {

	@Test // INT-568
	public void testApplicationContextCreation() {
	}

	public static class TestService {

		public String getVal() {
			return "fooble";
		}

	}

	public static class TestEndpoint {

		private TestService service;

		@Autowired
		public TestEndpoint(TestService service) {
			this.service = service;
		}

		public String aProducer() {
			return this.service.getVal();
		}

		public void aConsumer(String str) {
			// ignore
		}

		public List<String> aSplitter(List<String> strs) {
			return strs;
		}

	}

}

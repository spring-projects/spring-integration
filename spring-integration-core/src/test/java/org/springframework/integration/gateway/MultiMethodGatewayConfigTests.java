/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.gateway;

import org.junit.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @since 2.0.M1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MultiMethodGatewayConfigTests {

	@Autowired
	private ApplicationContext applicationContext;


	@Test
	public void validateGatewayMethods() {
		TestGateway gateway = (TestGateway) applicationContext.getBean("myGateway");
		String parentClassName = "org.springframework.integration.gateway.MultiMethodGatewayConfigTests";
		Assert.assertEquals(gateway.echo("oleg"),
				parentClassName + "$TestBeanA:oleg");
		Assert.assertEquals(gateway.echoUpperCase("oleg"),
				parentClassName + "$TestBeanB:oleg");
		Assert.assertEquals(gateway.echoViaDefault("oleg"),
				parentClassName + "$TestBeanC:oleg");
	}


	public static class TestBeanA {
		public String echo(String str) {
			return this.getClass().getName() + ":" + str;
		}
	}

	public static class TestBeanB {
		public String echo(String str) {
			return this.getClass().getName() + ":" + str;
		}
	}

	public static class TestBeanC {
		public String echo(String str) {
			return this.getClass().getName() + ":" + str;
		}
	}

	public interface TestGateway {
		String echo(String str);
		String echoViaDefault(String str);
		String echoUpperCase(String str);
	}

}

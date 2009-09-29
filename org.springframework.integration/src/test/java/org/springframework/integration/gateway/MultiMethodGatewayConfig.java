/*
 * Copyright 2002-2008 the original author or authors.
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

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @since 2.0.M1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MultiMethodGatewayConfig {
	
	@Autowired
	private ApplicationContext applicationContext;
	@Test
	public void validateGatewayMethods(){
		TestGateway gateway = (TestGateway) applicationContext.getBean("myGateway");
		Assert.assertEquals(gateway.echo("oleg"), 
						"org.springframework.integration.gateway.MultiMethodGatewayConfig$TestBeanA:oleg");
		Assert.assertEquals(gateway.echoUpperCase("oleg"), 
						"org.springframework.integration.gateway.MultiMethodGatewayConfig$TestBeanB:oleg");
		Assert.assertEquals(gateway.echoViaDefault("oleg"), 
						"org.springframework.integration.gateway.MultiMethodGatewayConfig$TestBeanC:oleg");
	}
	
	public static class TestBeanA{
		public String echo(String str){
			return this.getClass().getName() + ":" + str;
		}
	}
	public static class TestBeanB{
		public String echo(String str){
			return this.getClass().getName() + ":" + str;
		}
	}
	public static class TestBeanC{
		public String echo(String str){
			return this.getClass().getName() + ":" + str;
		}
	}
	
	public static interface TestGateway{
		public String echo(String str);
		public String echoViaDefault(String str);
		public String echoUpperCase(String str);
	}
}

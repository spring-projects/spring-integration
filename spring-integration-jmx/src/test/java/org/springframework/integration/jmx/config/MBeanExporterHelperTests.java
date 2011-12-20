/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jmx.config;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @author Oleg Zhurakousky
 *
 */
public class MBeanExporterHelperTests {

	@Test
	public void testNoNpeWhenClassNameIsMissing(){
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition bd = new RootBeanDefinition();
		factory.registerBeanDefinition("foo", bd);
		
		MBeanExporterHelper helper = new MBeanExporterHelper();
		helper.postProcessBeanFactory(factory);
	}
	
	@Test
	public void testNoNpeWithAbstractBean(){
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		RootBeanDefinition abstractBd = new RootBeanDefinition();
		abstractBd.setAbstract(true);
		abstractBd.setBeanClass(String.class);
		factory.registerBeanDefinition("abstractFoo", abstractBd);
		RootBeanDefinition concreteBd = new RootBeanDefinition(abstractBd);
		
		factory.registerBeanDefinition("concreteFoo", concreteBd);
		
		MBeanExporterHelper helper = new MBeanExporterHelper();
		helper.postProcessBeanFactory(factory);
	}
}

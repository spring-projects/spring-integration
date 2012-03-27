/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.config.xml;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
 * 
 * @author Oleg Zhurakousky
 *
 */
public class TestDisableChannelAutoCreationTests {

	@Test(expected=BeanCreationException.class)
	public void testDisablingAutoChannelCreation(){
		new ClassPathXmlApplicationContext("TestDisableChannelAutoCreation-context.xml", this.getClass());
	}
}

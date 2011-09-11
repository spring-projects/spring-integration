/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.file.remote.session;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.Message;

/**
 * Uses a SpEL expression on the message to determine the bean name of the
 * {@link SessionFactory}.
 * 
 * @author Gary Russell
 * @since 2.1
 *
 */
public class BeanNameSessionFactoryResolver implements SessionFactoryResolver, ApplicationContextAware {

	public SessionFactory resolve(Message<?> message) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		// TODO Auto-generated method stub
		
	}
	
	public void setSessionFactoryBeanNameExpression(String expression) {
		
	}

}

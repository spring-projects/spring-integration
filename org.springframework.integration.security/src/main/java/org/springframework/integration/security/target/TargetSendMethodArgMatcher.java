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
package org.springframework.integration.security.target;

import java.lang.reflect.Method;

import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.integration.message.Message;

/**
 * 
 * @author Jonas Partner
 *
 */
public class TargetSendMethodArgMatcher extends StaticMethodMatcher{
	

	@SuppressWarnings("unchecked")
	public boolean matches(Method method, Class targetClass) {
		return argsTypesMatch(method.getParameterTypes());
	}


	
	@SuppressWarnings("unchecked")
	protected boolean argsTypesMatch(Class[] args){
		if(args.length > 2){
			return false;
		}
		
		if(args.length > 0){
			if(!Message.class.isAssignableFrom(args[0] )){
				return false;
			}
		} 
		
		if (args.length > 1 ){
			if(!long.class.isAssignableFrom(args[1])){
				return false;
			}
		}
		return true;
		
		
	}
}
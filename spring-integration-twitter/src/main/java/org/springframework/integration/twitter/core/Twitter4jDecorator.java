/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.twitter.core;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import twitter4j.DirectMessage;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.User;

/**
 * Sine our Twitter domain model is modeled after Twitter4j API
 * this interceptor will delegate method calls that are made on SI Twitter interfaces
 * to the corresponding Twitter4j objects.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class Twitter4jDecorator implements MethodInterceptor {
	
	private final Object twitterObject;
	
	public Twitter4jDecorator(Object twitterObject){
		this.validateTwitter4JObject(twitterObject);
		this.twitterObject = twitterObject;
	}

	/* (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Class<?> twitter4jClass = twitterObject.getClass();
		Object[] args = invocation.getArguments();
		Class<?>[] argsType = new Class[args.length];
		for (int i = 0; i < args.length; i++) {
			Object object = args[i];
			argsType[i] = object.getClass();
		}
		String invokedmethodName = invocation.getMethod().getName();
		Method targetMethod = ReflectionUtils.findMethod(twitter4jClass, invokedmethodName, argsType);
		return targetMethod.invoke(twitterObject, args);
	}

	private void validateTwitter4JObject(Object twitterObject){
		Assert.notNull(twitterObject, "'twitterObject' must not be null");
		Assert.isTrue(twitterObject instanceof Status ||
					  twitterObject instanceof DirectMessage ||
					  twitterObject instanceof User ||
					  twitterObject instanceof GeoLocation, 
					  "Only twitter4j.DirectMessage, twitter4j.Status, twitter4j.User, twitter4j.GeoLocation is supported");
	}
}

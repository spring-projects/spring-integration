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

package org.springframework.integration.util;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of {@link MethodInvoker} to be used when the actual {@link Method} reference is known.
 * 
 * @author Mark Fisher
 */
public class DefaultMethodInvoker implements MethodInvoker {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Object object;

	private final Method method;

	private volatile TypeConverter typeConverter;


	public DefaultMethodInvoker(Object object, Method method) {
		Assert.notNull(object, "object must not be null");
		Assert.notNull(method, "method must not be null");
		this.object = object;
		this.method = method;
	}


	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	protected TypeConverter getTypeConverter() {
		if (this.typeConverter == null) {
			this.typeConverter = new SimpleTypeConverter();
		}
		return this.typeConverter;
	}

	public Object invokeMethod(Object ... args) throws Exception {
		TypeConverter converter = getTypeConverter();
		int argCount = args.length;
		Class<?>[] paramTypes = this.method.getParameterTypes();
		if (paramTypes.length != argCount) {
			throw new IllegalArgumentException("Wrong number of arguments. Expected types " + 
					ObjectUtils.nullSafeToString(paramTypes) + ", but received values " +
					ObjectUtils.nullSafeToString(args) + ".");
		}
		Object[] convertedArgs = new Object[argCount];
		boolean match = true;
		for (int i = 0; i < argCount && match; i++) {
			try {
				convertedArgs[i] = converter.convertIfNecessary(args[i], paramTypes[i]);
			}
			catch (TypeMismatchException e) {
				throw new IllegalArgumentException("Failed to convert argument type.", e);
			}
		}
		this.method.setAccessible(true);
		if (this.logger.isDebugEnabled()) {
			logger.debug("invoking method '" + this.method.getName() + "' with arguments " + ObjectUtils.nullSafeToString(convertedArgs));
		}
		return this.method.invoke(this.object, convertedArgs);
	}

}

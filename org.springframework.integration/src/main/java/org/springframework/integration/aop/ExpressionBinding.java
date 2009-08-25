/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that provides the variable names to use when constructing the
 * evaluation context for a MessagePublishingInterceptor.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExpressionBinding {

	/**
	 * Names of the arguments as a comma-separated list. If not provided, the
	 * names will be discovered automatically if enabled by the compiler settings.
	 * These names will be used as the keys in the argument Map.
	 */
	String argNames() default "";

	/**
	 * Name of the variable in the context that refers to the Map of arguments.
	 * <p>The default is "args". 
	 */
	String argumentMapName() default ExpressionSource.DEFAULT_ARGUMENT_MAP_NAME;

	/**
	 * Name of the variable in the context that refers to the return value, if any.
	 * <p>The default is "return".
	 */
	String returnValueName() default ExpressionSource.DEFAULT_RETURN_VALUE_NAME;

	/**
	 * Name of the variable in the context that refers to any exception thrown
	 * by the method invocation that is being intercepted.
	 * <p>The default is "exception".
	 */
	String exceptionName() default ExpressionSource.DEFAULT_EXCEPTION_NAME;

}

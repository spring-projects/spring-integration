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

package org.springframework.integration.handler;

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * An adaptation of the SpEL ReflectiveMethodResolver with the simple addition of a {@link MethodFilter}.
 *  
 * @author Andy Clement
 * @author Mark Fisher
 * @since 2.0
 */
class FilteringReflectiveMethodResolver implements MethodResolver {

	private final MethodFilter methodFilter;

	private final Class<?>[] filteredTypes;


	public FilteringReflectiveMethodResolver(MethodFilter methodFilter, Class<?> ... filteredTypes) {
		Assert.notNull(methodFilter, "methodFilter must not be null");
		this.methodFilter = methodFilter;
		this.filteredTypes = filteredTypes;
	}


	public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, Class<?>[] argumentTypes) throws AccessException {
		try {
			TypeConverter typeConverter = context.getTypeConverter();
			Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());

			// this is the only addition for this implementation:
			Method[] methods = ObjectUtils.containsElement(this.filteredTypes, type) ?
					this.methodFilter.filter(type.getMethods()) : type.getMethods();

			Method closeMatch = null;
			int[] argsToConvert = null;
			boolean multipleOptions = false;
			Method matchRequiringConversion = null;
			for (Method method : methods) {
				if (method.isBridge()) {
					continue;
				}
				if (method.getName().equals(name)) {
					ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
					if (method.isVarArgs() && argumentTypes.length >= (method.getParameterTypes().length - 1)) {
						// *sigh* complicated
						matchInfo = ReflectionHelper.compareArgumentsVarargs(method.getParameterTypes(), argumentTypes, typeConverter);
					} else if (method.getParameterTypes().length == argumentTypes.length) {
						// name and parameter number match, check the arguments
						matchInfo = ReflectionHelper.compareArguments(method.getParameterTypes(), argumentTypes, typeConverter);
					}
					if (matchInfo != null) {
						if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.EXACT) {
							return new ReflectiveMethodExecutor(method, null);
						}
						else if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.CLOSE) {
							closeMatch = method;
						}
						else if (matchInfo.kind == ReflectionHelper.ArgsMatchKind.REQUIRES_CONVERSION) {
							if (matchRequiringConversion != null) {
								multipleOptions = true;
							}
							argsToConvert = matchInfo.argsRequiringConversion;
							matchRequiringConversion = method;
						}
					}
				}
			}
			if (closeMatch != null) {
				return new ReflectiveMethodExecutor(closeMatch, null);
			}
			else if (matchRequiringConversion != null) {
				if (multipleOptions) {
					throw new SpelEvaluationException(SpelMessage.MULTIPLE_POSSIBLE_METHODS, name);
				}
				return new ReflectiveMethodExecutor(matchRequiringConversion, argsToConvert);
			}
			else {
				return null;
			}
		}
		catch (EvaluationException ex) {
			throw new AccessException("Failed to resolve method", ex);
		}
	}


	/**
	 * This class is copied also, since the original has package-only access.
	 * 
	 * @author Andy Clement
	 */
	private static class ReflectiveMethodExecutor implements MethodExecutor {

		private final Method method;

		// When the method was found, we will have determined if arguments need to be converted for it
		// to be invoked. Conversion won't be cheap so let's only do it if necessary.
		private final int[] argsRequiringConversion;


		public ReflectiveMethodExecutor(Method theMethod, int[] argumentsRequiringConversion) {
			this.method = theMethod;
			this.argsRequiringConversion = argumentsRequiringConversion;
		}


		public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
			try {
				if (this.argsRequiringConversion != null && arguments != null) {
					ReflectionHelper.convertArguments(this.method.getParameterTypes(), this.method.isVarArgs(),
							context.getTypeConverter(), this.argsRequiringConversion, arguments);
				}
				if (this.method.isVarArgs()) {
					arguments = ReflectionHelper.setupArgumentsForVarargsInvocation(this.method.getParameterTypes(), arguments);
				}
				ReflectionUtils.makeAccessible(this.method);
				return new TypedValue(this.method.invoke(target, arguments), new TypeDescriptor(new MethodParameter(method,-1)));
			} catch (Exception ex) {
				throw new AccessException("Problem invoking method: " + this.method, ex);
			}
		}

	}

}

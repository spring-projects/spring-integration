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

package org.springframework.integration.adapter;

import java.lang.reflect.Method;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.Target;
import org.springframework.integration.util.MethodValidator;

/**
 * A messaging target that invokes the specified method on the provided object.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingTarget extends MethodInvokingHandler implements Target {

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		this.invoker.setMethodValidator(new MethodValidator() {
			public void validate(Method method) throws Exception {
				if (!method.getReturnType().equals(void.class)) {
					throw new ConfigurationException("target method must have a void return");
				}
			}
		});
	}

	public boolean send(Message<?> message) {
		this.handle(message);
		return true;
	}

}

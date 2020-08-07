/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.endpoint;

import java.lang.reflect.Method;

import org.springframework.context.Lifecycle;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link org.springframework.integration.core.MessageSource} implementation that
 * invokes a no-argument method so that its return value may be sent to a channel.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MethodInvokingMessageSource extends AbstractMessageSource<Object> implements ManageableLifecycle {

	private volatile Object object;

	private volatile Method method;

	private volatile String methodName;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public void setObject(Object object) {
		Assert.notNull(object, "'object' must not be null");
		this.object = object;
	}

	public void setMethod(Method method) {
		Assert.notNull(method, "'method' must not be null");
		this.method = method;
	}

	public void setMethodName(String methodName) {
		Assert.notNull(methodName, "'methodName' must not be null");
		this.methodName = methodName;
	}

	@Override
	public String getComponentType() {
		return "inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.object, "object is required");
			Assert.isTrue(this.method != null || this.methodName != null, "method or methodName is required");
			if (this.method == null) {
				this.method = ReflectionUtils.findMethod(this.object.getClass(), this.methodName);
				Assert.notNull(this.method, "no such method '" + this.methodName
						+ "' is available on " + this.object.getClass());
			}
			Assert.isTrue(!void.class.equals(this.method.getReturnType()),
					"invalid MessageSource method '" + this.method.getName() + "', a non-void return is required");
			this.method.setAccessible(true);
			this.initialized = true;
		}
	}

	@Override
	public void start() {
		if (this.object instanceof Lifecycle) {
			((Lifecycle) this.object).start();
		}
	}

	@Override
	public void stop() {
		if (this.object instanceof Lifecycle) {
			((Lifecycle) this.object).stop();
		}
	}

	@Override
	public boolean isRunning() {
		return !(this.object instanceof Lifecycle) || ((Lifecycle) this.object).isRunning();
	}

	@Override
	protected Object doReceive() {
		try {
			if (!this.initialized) {
				this.afterPropertiesSet();
			}
			return ReflectionUtils.invokeMethod(this.method, this.object);
		}
		catch (Exception e) {
			throw new MessagingException("Failed to invoke method", e);
		}
	}

}

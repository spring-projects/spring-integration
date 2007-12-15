/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.springframework.integration.message.MessageMapper;
import org.springframework.util.Assert;

/**
 * An outbound channel adapter for invoking the specified method on the provided
 * object. Delegates to a {@link MessageMapper} for converting between objects
 * and messages. An optional {@link ArgumentListPreparer} may also be provided.
 * 
 * @author Mark Fisher
 */
public class OutboundMethodInvokingChannelAdapter<T> extends AbstractOutboundChannelAdapter {

	private T object;

	private String method;

	private SimpleMethodInvoker<T> invoker;

	private ArgumentListPreparer argumentListPreparer;


	public void setObject(T object) {
		Assert.notNull(object, "'object' must not be null");
		this.object = object;
	}

	public void setMethod(String method) {
		Assert.notNull(method, "'method' must not be null");
		this.method = method;
	}

	public void setArgumentListPreparer(ArgumentListPreparer argumentListPreparer) {
		this.argumentListPreparer = argumentListPreparer;
	}

	@Override
	public void initialize() {
		this.invoker = new SimpleMethodInvoker<T>(this.object, this.method);
	}

	@Override
	public boolean doSendObject(Object object) throws Exception {
		Object args[] = null;
		if (this.argumentListPreparer != null) {
			args = this.argumentListPreparer.prepare(object);
		}
		else {
			args = new Object[] { object };
		}
		Object result = this.invoker.invokeMethod(args);
		if (result != null && logger.isWarnEnabled()) {
			logger.warn("ignoring outbound channel adapter's return value");
		}
		return true;
	}

}

/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.support.management;

import org.springframework.context.Lifecycle;

/**
 * An extension to {@link LifecycleMessageSourceMetrics} for sources that implement {@link MessageSourceManagement}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 * @deprecated in favor of Micrometer metrics.
 */
@Deprecated
@SuppressWarnings("deprecation")
public class LifecycleMessageSourceManagement extends LifecycleMessageSourceMetrics implements MessageSourceManagement {

	public LifecycleMessageSourceManagement(Lifecycle lifecycle, MessageSourceManagement delegate) {
		super(lifecycle, delegate);
	}

	@Override
	public void setMaxFetchSize(int maxFetchSize) {
		((MessageSourceManagement) this.delegate).setMaxFetchSize(maxFetchSize);
	}

	@Override
	public int getMaxFetchSize() {
		return ((MessageSourceManagement) this.delegate).getMaxFetchSize();
	}

}

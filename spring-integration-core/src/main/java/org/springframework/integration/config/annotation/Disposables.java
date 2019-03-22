/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.DisposableBean;

/**
 * A container for a collection of {@link DisposableBean} it is, itself a
 * {@link DisposableBean} and will dispose of its contained beans when it is destroyed.
 * Intended for any {@link DisposableBean} that is registered as a singleton in which
 * case, the container does not automatically dispose of them.
 *
 * @author Gary Russell
 * @since 5.1
 *
 */
class Disposables implements DisposableBean {

	private final List<DisposableBean> disposables = new ArrayList<>();

	public void add(DisposableBean... disposables) {
		this.disposables.addAll(Arrays.asList(disposables));
	}

	@Override
	public void destroy() throws Exception {
		this.disposables.forEach(d -> {
			try {
				d.destroy();
			}
			catch (Exception e) {
				// NOSONAR
			}
		});
	}
}

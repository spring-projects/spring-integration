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

package org.springframework.integration.adapter;

import org.springframework.integration.message.MessageMapper;
import org.springframework.util.Assert;

/**
 * Target adapter implementation that delegates to a {@link MessageMapper}
 * and then passes the resulting object to the provided {@link Target}.
 * 
 * @author Mark Fisher
 */
public class DefaultTargetAdapter<T> extends AbstractTargetAdapter<T> {

	private Target<T> target;


	public DefaultTargetAdapter(Target<T> target) {
		Assert.notNull(target, "'target' must not be null");
		this.target = target;
	}

	public boolean sendToTarget(T object) {
		return this.target.send(object);
	}

}

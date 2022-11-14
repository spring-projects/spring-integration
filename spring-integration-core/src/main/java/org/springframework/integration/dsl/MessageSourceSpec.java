/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.util.Assert;

/**
 * An {@link IntegrationComponentSpec} for {@link MessageSource}s.
 *
 * @param <S> the target {@link MessageSourceSpec} implementation type.
 * @param <H> the target {@link MessageSource} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class MessageSourceSpec<S extends MessageSourceSpec<S, H>, H extends MessageSource<?>>
		extends IntegrationComponentSpec<S, H> {

	/**
	 * Expressions with which to enhance headers.
	 * Only applies to subclasses of {@link AbstractMessageSource}.
	 * @param headerExpressions the header expressions.
	 * @return the spec.
	 * @since 5.0.1
	 */
	public S messageHeaders(Map<String, Expression> headerExpressions) {
		Assert.state(this.target instanceof AbstractMessageSource,
				() -> "'MessageSource' must be an instance of 'AbstractMessageSource', not "
						+ this.target.getClass());
		((AbstractMessageSource<?>) this.target).setHeaderExpressions(headerExpressions);
		return _this();
	}

}

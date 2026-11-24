/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

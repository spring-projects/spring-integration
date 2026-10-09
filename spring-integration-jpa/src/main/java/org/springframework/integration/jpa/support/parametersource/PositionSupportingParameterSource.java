/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.support.parametersource;

import org.jspecify.annotations.Nullable;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public interface PositionSupportingParameterSource extends ParameterSource {

	@Nullable
	Object getValueByPosition(int position);

}

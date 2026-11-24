/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

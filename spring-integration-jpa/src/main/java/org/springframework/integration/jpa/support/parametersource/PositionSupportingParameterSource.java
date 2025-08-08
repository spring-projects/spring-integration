/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.support.parametersource;

/**
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public interface PositionSupportingParameterSource extends ParameterSource {

	Object getValueByPosition(int position);

}

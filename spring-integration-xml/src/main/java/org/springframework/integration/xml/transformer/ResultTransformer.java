/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.transformer;

import javax.xml.transform.Result;

/**
 * Implementations of this class allow for the transformation of {@link Result}
 * objects to other formats.
 *
 * @author Jonas Partner
 * @author Gunnar Hillert
 *
 */
public interface ResultTransformer {

	Object transformResult(Result result);

}

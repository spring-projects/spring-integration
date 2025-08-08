/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.result;

import javax.xml.transform.Result;

/**
 * Factory to create a {@link Result} possibly taking into account the
 * provided message payload instance.
 *
 * @author Jonas Partner
 */
public interface ResultFactory {

	Result createResult(Object payload);

}

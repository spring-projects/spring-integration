/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.support.management;

import org.springframework.context.SmartLifecycle;

/**
 * Extend {@link ManageableLifecycle} to make those methods manageable.
 *
 * @author Gary Russell
 * @since 5.4
 *
 */
public interface ManageableSmartLifecycle extends SmartLifecycle, ManageableLifecycle {

}

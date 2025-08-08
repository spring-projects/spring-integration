/*
 * Copyright © 2021 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2021-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.util.function.BiFunction;

import org.springframework.messaging.Message;

/**
 * A contract which can be implemented on the {@link ReleaseStrategy}
 * and used in the {@link AbstractCorrelatingMessageHandler} to
 * populate the provided group condition supplier.
 *
 * @author Artem Bilan
 *
 * @since 5.5
 *
 * @see AbstractCorrelatingMessageHandler#setGroupConditionSupplier(BiFunction)
 */
@FunctionalInterface
public interface GroupConditionProvider {

	BiFunction<Message<?>, String, String> getGroupConditionSupplier();

}

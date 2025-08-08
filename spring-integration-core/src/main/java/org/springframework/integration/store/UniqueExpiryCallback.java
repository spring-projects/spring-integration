/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.store;

/**
 * A marker interface extension of the {@link MessageGroupStore.MessageGroupCallback}
 * for components which should be registered in the {@link MessageGroupStore} only once.
 * The {@link MessageGroupStore} implementation ensures that only once instance of this
 * class is present in the expire callbacks.
 *
 * @author Meherzad Lahewala
 * @author Artme Bilan
 *
 * @since 5.0.10
 */
@FunctionalInterface
public interface UniqueExpiryCallback extends MessageGroupStore.MessageGroupCallback {

}

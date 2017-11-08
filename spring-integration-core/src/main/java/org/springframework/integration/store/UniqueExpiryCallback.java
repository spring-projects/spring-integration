package org.springframework.integration.store;

import org.springframework.integration.store.MessageGroupStore.MessageGroupCallback;

/**
 * Invoked when a MessageGroupStore expires a group. A message store ensure's
 * only one instance of UniqueExpiryCallback can be registered.
 * 
 * @author Meherzad Lahewala
 * @since 5.0
 */
@FunctionalInterface
public interface UniqueExpiryCallback extends MessageGroupCallback {

}

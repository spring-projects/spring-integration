package org.springframework.integration.dispatcher;

import org.springframework.integration.core.Message;

public abstract class AbstractSendOnceDispatcher extends AbstractDispatcher {

	public abstract boolean dispatch(Message<?> message);

}

package org.springframework.integration.dispatcher;

import java.util.Iterator;
import java.util.List;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageRejectedException;

/**
 * Implementation of {@link MessageDispatcher} that will attempt to send a
 * {@link Message} to at most one of its handlers. The handlers will be tried
 * one by one. As soon as <em>one</em> of the handlers accepts the Message, the
 * dispatcher will return <code>true</code> and ignore the rest of it's
 * handlers.
 * <p/>
 * If the dispatcher has no handlers, a {@link MessageDeliveryException} will be
 * thrown. If all handlers reject the Message, the dispatcher will throw a
 * MessageRejectedException.
 * <p/>
 * The implementations of this class control the order in which the handlers
 * will be tried through the implementation of the {@link #getHandlerIterator(List)} method.
 * 
 * @author Iwein Fuld
 * 
 */
public abstract class AbstractHandleOnceDispatcher extends AbstractDispatcher {

	public final boolean dispatch(Message<?> message) {
		boolean success = false;
		List<MessageHandler> handlers = this.getHandlers();
		if (handlers.isEmpty()) {
			throw new MessageDeliveryException(message, "Dispatcher has no subscribers.");
		}
		Iterator<MessageHandler> handlerIterator = getHandlerIterator(handlers);
		while (success == false && handlerIterator.hasNext()) {
			MessageHandler handler = handlerIterator.next();
			if (this.sendMessageToHandler(message, handler)) {
				success = true; // we have a winner.
			}
		}
		if (!success) {
			throw new MessageRejectedException(message, "All of dispatcher's subscribers rejected Message.");
		}
		return success;
	}

	/**
	 * Return the iterator that will be used to loop over the handlers. This
	 * allows subclasses to control the order of iteration for each
	 * {@link #dispatch(Message)} invocation.
	 * @param handlers
	 * @return
	 */
	protected abstract Iterator<MessageHandler> getHandlerIterator(List<MessageHandler> handlers);
}

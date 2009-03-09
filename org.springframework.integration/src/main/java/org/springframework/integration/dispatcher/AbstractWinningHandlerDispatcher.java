package org.springframework.integration.dispatcher;

import java.util.ArrayList;
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
 * will be tried through the <code>getNextHandler</code> method.
 * 
 * @author Iwein Fuld
 * 
 */
public abstract class AbstractWinningHandlerDispatcher extends AbstractDispatcher {

	public final boolean dispatch(Message<?> message) {
		boolean success = false;
		List<MessageHandler> triedHandlers = new ArrayList<MessageHandler>();
		int size;
		List<MessageHandler> handlers = this.getHandlers();
		if (handlers.isEmpty()) {
			throw new MessageDeliveryException(message, "Dispatcher has no subscribers.");
		}
		size = handlers.size();
		for (int i = 0; triedHandlers.size() < size && success == false; i++) {
			MessageHandler handler = handlers.get(getNextHandlerIndex(size, i));
			if (this.sendMessageToHandler(message, handler)) {
				success = true; //we have a winner.
			}
			triedHandlers.add(handler);
		}
		if (!success) {
			throw new MessageRejectedException(message, "All of dispatcher's subscribers rejected Message.");
		}
		return success;
	}

	/**
	 * Return the next handler index. Subclasses have to implement this method
	 * to determine the order in which handlers will be invoked.
	 * 
	 * @param size the total number of handlers
	 * @param loopIndex the current index of the loop
	 */
	protected abstract int getNextHandlerIndex(int size, int loopIndex);

}

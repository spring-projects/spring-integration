package org.springframework.integration.aggregator;

import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.store.MessageGroup;

/**
 * A {@link MessageGroupProcessor} implementation that evaluates a SpEL expression. The SpEL context root is the list of
 * all Messages in the group. The evaluation result can be any Object and is send as new Message payload to the output
 * channel.
 * 
 * @author Alex Peters
 * @author Dave Syer
 * 
 */
public class ExpressionEvaluatingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor implements BeanFactoryAware {
	
	private final ExpressionEvaluatingMessageListProcessor processor;

	public void setBeanFactory(BeanFactory beanFactory) {
		processor.setBeanFactory(beanFactory);
	}

	public void setConversionService(ConversionService conversionService) {
		processor.setConversionService(conversionService);
	}

	public void setExpectedType(Class<?> expectedType) {
		processor.setExpectedType(expectedType);
	}

	public ExpressionEvaluatingMessageGroupProcessor(String expression) {
		processor = new ExpressionEvaluatingMessageListProcessor(expression);
	}

	/**
	 * Evaluate the expression provided on the unmarked messages (a collection) in the group, and delegate to the
	 * {@link MessagingTemplate} to send downstream.
	 */
	@Override
	protected Object aggregatePayloads(MessageGroup group, Map<String, Object> headers) {
		return processor.process(group.getUnmarked());
	}

}

/**
 *
 */
package org.springframework.integration.util;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Oleg Zhurakousky
 *
 */
public class BeanFactoryTypeConverterTests {

	@SuppressWarnings("unchecked")
	@Test
	public void testEmptyCollectionConversion(){
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		List<String> sourceObject = new ArrayList<String>();
		ArrayList<BeanFactoryTypeConverterTests> convertedCollection =
			(ArrayList<BeanFactoryTypeConverterTests>) typeConverter.convertValue(sourceObject, TypeDescriptor.forObject(sourceObject), TypeDescriptor.forObject(new ArrayList<BeanFactoryTypeConverterTests>()));
		assertEquals(sourceObject, convertedCollection);
	}

	@Test
	public void testToStringConversion(){
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		String converted = (String) typeConverter.convertValue(new Integer(1234), TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(String.class));
		assertEquals("1234", converted);
	}

	@Test
	public void testToNonStringConversionNotSupportedByGenericConversionService(){
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		@SuppressWarnings("unchecked")
		Collection<Integer> converted = (Collection<Integer>) typeConverter.convertValue(new Integer(1234), TypeDescriptor.valueOf(Integer.class), TypeDescriptor.forObject(new ArrayList<Integer>(Arrays.asList(1))));
		assertEquals(Arrays.asList(1234), converted);
	}

	@Test
	public void testMessageHeadersNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		MessageHeaders headers = new GenericMessage<String>("foo").getHeaders();
		assertSame(headers, typeConverter.convertValue(headers, TypeDescriptor.valueOf(MessageHeaders.class), TypeDescriptor.valueOf(MessageHeaders.class)));
	}

	@Test
	public void testMessageHistoryNotConverted() {
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		typeConverter.setBeanFactory(new DefaultListableBeanFactory());
		Message<String> message = new GenericMessage<String>("foo");
		message = MessageHistory.write(message, new NamedComponent(){
			public String getComponentName() {
				return "bar";
			}

			public String getComponentType() {
				return "baz";
			}
		});
		MessageHistory history = MessageHistory.read(message);
		assertSame(history, typeConverter.convertValue(history, TypeDescriptor.valueOf(MessageHeaders.class), TypeDescriptor.valueOf(MessageHeaders.class)));
	}
}

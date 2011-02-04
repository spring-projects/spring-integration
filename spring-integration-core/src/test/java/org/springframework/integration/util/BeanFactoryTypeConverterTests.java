/**
 * 
 */
package org.springframework.integration.util;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.convert.TypeDescriptor;

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
		// source type doesn't even matter
		ArrayList<BeanFactoryTypeConverterTests> convertedCollection = 
			(ArrayList<BeanFactoryTypeConverterTests>) typeConverter.convertValue(sourceObject, null, TypeDescriptor.forObject(new ArrayList<BeanFactoryTypeConverterTests>()));
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

}

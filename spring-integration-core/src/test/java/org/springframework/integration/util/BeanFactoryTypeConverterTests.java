/**
 * 
 */
package org.springframework.integration.util;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Oleg Zhurakousky
 *
 */
public class BeanFactoryTypeConverterTests {

	@Test
	public void testEmptyCollectionConversion(){
		BeanFactoryTypeConverter typeConverter = new BeanFactoryTypeConverter();
		List<String> sourceObject = new ArrayList<String>();
		// source type doesn't even matter
		ArrayList<BeanFactoryTypeConverterTests> convertedCollection = 
			(ArrayList<BeanFactoryTypeConverterTests>) typeConverter.convertValue(sourceObject, null, TypeDescriptor.forObject(new ArrayList<BeanFactoryTypeConverterTests>()));
		assertEquals(sourceObject, convertedCollection);
	}
}

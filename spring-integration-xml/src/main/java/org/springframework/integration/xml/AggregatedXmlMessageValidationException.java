/**
 * 
 */
package org.springframework.integration.xml;

import java.util.Iterator;
import java.util.List;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AggregatedXmlMessageValidationException extends RuntimeException {
	
	private final List<Throwable> exceptions;

	public AggregatedXmlMessageValidationException(List<Throwable> exceptions){
		this.exceptions = exceptions;
	}
	/**
	 * Will return iterator of exceptions aggregated by this Class.
	 * 
	 * @return
	 */
	public Iterator<Throwable> exceptionIterator(){
		return exceptions.iterator();
	}
}

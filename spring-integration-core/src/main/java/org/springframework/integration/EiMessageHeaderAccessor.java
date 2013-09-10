package org.springframework.integration;

import java.util.Date;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.Assert;

public class EiMessageHeaderAccessor extends MessageHeaderAccessor {

    public static final String CORRELATION_ID = "correlationId";

    public static final String EXPIRATION_DATE = "expirationDate";

    public static final String PRIORITY = "priority";

    public static final String SEQUENCE_NUMBER = "sequenceNumber";

    public static final String SEQUENCE_SIZE = "sequenceSize";

    public static final String SEQUENCE_DETAILS = "sequenceDetails";

    public static final String POSTPROCESS_RESULT = "postProcessResult";

    public EiMessageHeaderAccessor(Message<?> message) {
    	super(message);
    }

    public Long getExpirationDate() {
        return this.getHeader(EXPIRATION_DATE, Long.class);
    }

    public Object getCorrelationId() {
        return this.getHeader(CORRELATION_ID);
    }

    public Integer getSequenceNumber() {
        Integer sequenceNumber = this.getHeader(SEQUENCE_NUMBER, Integer.class);
        return (sequenceNumber != null ? sequenceNumber : 0);
    }

    public Integer getSequenceSize() {
        Integer sequenceSize = this.getHeader(SEQUENCE_SIZE, Integer.class);
        return (sequenceSize != null ? sequenceSize : 0);
    }

    public Integer getPriority() {
        return this.getHeader(PRIORITY, Integer.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T getHeader(String key, Class<T> type) {
        Object value = getHeader(key);
        if (value == null) {
            return null;
        }
        if (!type.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Incorrect type specified for header '" + key + "'. Expected [" + type
                    + "] but actual type is [" + value.getClass() + "]");
        }
        return (T) value;
    }

    protected void verifyType(String headerName, Object headerValue) {
		if (headerName != null && headerValue != null) {
			super.verifyType(headerName, headerValue);
			if (EiMessageHeaderAccessor.EXPIRATION_DATE.equals(headerName)) {
				Assert.isTrue(headerValue instanceof Date || headerValue instanceof Long, "The '" + headerName
						+ "' header value must be a Date or Long.");
			}
			else if (EiMessageHeaderAccessor.SEQUENCE_NUMBER.equals(headerName)
					|| EiMessageHeaderAccessor.SEQUENCE_SIZE.equals(headerName)) {
				Assert.isTrue(Integer.class.isAssignableFrom(headerValue.getClass()), "The '" + headerName
						+ "' header value must be an Integer.");
			}
			else if (EiMessageHeaderAccessor.PRIORITY.equals(headerName)) {
				Assert.isTrue(Integer.class.isAssignableFrom(headerValue.getClass()), "The '" + headerName
						+ "' header value must be an Integer.");
			}
		}
	}
}

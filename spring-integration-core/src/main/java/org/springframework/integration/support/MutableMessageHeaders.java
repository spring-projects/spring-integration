package org.springframework.integration.support;

import org.springframework.messaging.MessageHeaders;

import java.util.Map;
import java.util.UUID;


/**
 * A MessageHeaders that permits direct access to and modification of the
 * header map.
 *
 * @author Stuart Williams
 * @since 4.1
 */
class MutableMessageHeaders extends MessageHeaders {

    private static final long serialVersionUID = 3084692953798643018L;

    /**
     * @param headers map
     */
    public MutableMessageHeaders(Map<String, Object> headers) {
        super(headers);
    }

    /**
     * @param headers map
     * @param id of message
     * @param timestamp of message
     */
    public MutableMessageHeaders(Map<String, Object> headers, UUID id, Long timestamp) {
        super(headers, id, timestamp);
    }

    @Override
    public Map<String, Object> getRawHeaders() {
        return super.getRawHeaders();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map) {
        super.getRawHeaders().putAll(map);
    }

    @Override
    public Object put(String key, Object value) {
        return super.getRawHeaders().put(key, value);
    }

    @Override
    public void clear() {
        super.getRawHeaders().clear();
    }

    @Override
    public Object remove(Object key) {
        return super.getRawHeaders().remove(key);
    }

}

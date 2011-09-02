package org.springframework.integration.test.support;
/**
 * Validate a message payload. Create an anonymous instance or subclass this 
 * to validate a response payload.
 * @author David Turanski
 *
 */
public abstract class PayloadValidator extends AbstractResponseValidator {
    protected final boolean extractPayload(){
        return true;
    }
}

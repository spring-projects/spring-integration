package org.springframework.integration.xml.transformer;


import org.springframework.integration.xml.result.ResultFactory;

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class CustomTestResultFactory implements ResultFactory {

    private final String stringToReturn;


    public CustomTestResultFactory(String stringToReturn) {
        this.stringToReturn = stringToReturn;
    }

    public Result createResult(Object payload) {
        return new FixedStringResult(this.stringToReturn);  //To change body of implemented methods use File | Settings | File Templates.
    }

    public static class FixedStringResult extends StreamResult {


        private final String stringToReturn;

        public FixedStringResult(String stringToReturn) {
            super(new StringWriter());
            this.stringToReturn = stringToReturn;
        }

        /**
         * Returns the written XML as a string.
         */
        public String toString() {
            return this.stringToReturn;
        }

    }


}

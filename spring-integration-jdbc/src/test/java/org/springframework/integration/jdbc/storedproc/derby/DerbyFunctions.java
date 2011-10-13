package org.springframework.integration.jdbc.storedproc.derby;

import java.util.Locale;

/**
 *
 * @author Gunnar Hillert
 *
 */
public final class DerbyFunctions {

      public static String convertStringToUpperCase( String invalue ) {
                return invalue.toUpperCase(Locale.ENGLISH);
      }

}

package org.springframework.integration.jdbc.storedproc.h2;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.h2.tools.SimpleResultSet;
import org.hsqldb.Types;

/**
 * 
 * @author Gunnar Hillert
 *
 */
public final class H2StoredProcedures {

     public static ResultSet getPrimes(int beginRange, int endRange) throws SQLException {

            SimpleResultSet rs = new SimpleResultSet();
            rs.addColumn("PRIME", Types.INTEGER, 10, 0);

            for (int i = beginRange; i <= endRange; i++) {

                if (new BigInteger(String.valueOf(i)).isProbablePrime(100)) {
                    rs.addRow(i);
                }
            }

            return rs;
     }

     public static Integer random() {
         return 1 + (int)(Math.random() * 100);
     }

}

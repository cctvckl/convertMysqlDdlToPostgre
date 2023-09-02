package org.example;

import java.util.HashMap;

/**
 * https://www.postgresql.org/docs/11/functions-datetime.html#FUNCTIONS-DATETIME-CURRENT
 */
public class DefaultValueMapping {
    public static final HashMap<String, String> MYSQL_DEFAULT_TO_POSTGRE_DEFAULT
            = new HashMap<String, String>();

    static {
        MYSQL_DEFAULT_TO_POSTGRE_DEFAULT.put("NULL", "NULL");
        MYSQL_DEFAULT_TO_POSTGRE_DEFAULT.put("CURRENT_TIMESTAMP", "CURRENT_TIMESTAMP");
        MYSQL_DEFAULT_TO_POSTGRE_DEFAULT.put("CURRENT_DATE", "CURRENT_DATE");
        MYSQL_DEFAULT_TO_POSTGRE_DEFAULT.put("CURRENT_TIME", "CURRENT_TIME");
    }
}

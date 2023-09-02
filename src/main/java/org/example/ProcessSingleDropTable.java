package org.example;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.drop.Drop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProcessSingleDropTable {

    public static String process(Drop drop) throws JSQLParserException {
        String type = drop.getType();
        if (Objects.equals("TABLE", type)) {
            String tableName = drop.getName().toString();
            boolean ifExists = drop.isIfExists();
            String sql = String.format("DROP TABLE %s %s;",ifExists ? "IF EXISTS" : "", tableName);
            return sql;
        }
        throw new UnsupportedOperationException();
    }

}

package org.example;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.drop.Drop;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) throws JSQLParserException, IOException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream = contextClassLoader.getResourceAsStream("source-mysql-ddl.txt");
        if (inputStream == null) {
            throw new RuntimeException();
        }
        String sqlContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        sqlContent = sqlContent.replaceAll("`","");

        StringBuilder totalSql = new StringBuilder();
        Statements statements = CCJSqlParserUtil.parseStatements(sqlContent);
        for (Statement statement : statements.getStatements()) {
            if (statement instanceof CreateTable) {
                String sql = ProcessSingleCreateTable.process((CreateTable) statement);
                totalSql.append(sql).append("\n");
            } else if (statement instanceof Drop) {
                String sql = ProcessSingleDropTable.process((Drop) statement);
                totalSql.append(sql).append("\n");
            } else {
                throw new UnsupportedOperationException();
            }
        }

        File destFile = new File(System.getProperty("user.dir"), "target.sql");
        FileUtils.writeStringToFile(destFile,totalSql.toString(),StandardCharsets.UTF_8);
        System.out.println(totalSql);
        System.out.println("file saved to :" + destFile.getAbsolutePath());

    }

}

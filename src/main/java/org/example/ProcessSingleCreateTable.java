package org.example;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;

import javax.xml.stream.events.Characters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProcessSingleCreateTable {

    public static String process(CreateTable createTable) throws JSQLParserException {
        String tableFullyQualifiedName = createTable.getTable().getFullyQualifiedName();
        List<ColumnDefinition> columnDefinitions = createTable.getColumnDefinitions();

        /**
         * 生成目标sql：表注释
         */
        List<String> tableOptionsStrings = createTable.getTableOptionsStrings();
        String tableCommentSql = null;
        int commentIndex = tableOptionsStrings.indexOf("COMMENT");
        if (commentIndex != -1) {
            tableCommentSql = String.format("COMMENT ON TABLE %s IS %s;", tableFullyQualifiedName,
                    tableOptionsStrings.get(commentIndex + 2));
        }

        /**
         * 生成目标sql：列注释
         */
        List<String> columnComments = extractColumnCommentSql(tableFullyQualifiedName, columnDefinitions);

        /**
         * 获取主键
         */
        Index primaryKey = createTable.getIndexes().stream()
                .filter((Index index) -> Objects.equals("PRIMARY KEY", index.getType()))
                .findFirst().orElse(null);
        if (primaryKey == null) {
            throw new RuntimeException("Primary key not found");
        }

        /**
         * 生成目标sql：第一行的建表语句
         */
        String createTableFirstLine = String.format("CREATE TABLE %s (", tableFullyQualifiedName);

        /**
         * 生成目标sql：主键
         */
        String primaryKeyColumnSql = generatePrimaryKeySql(columnDefinitions, primaryKey);
        /**
         * 生成目标sql：除了主键之外的其他列
         */
        List<String> otherColumnSqlList = generateOtherColumnSql(columnDefinitions, primaryKey);


        String fullSql = generateFullSql(createTableFirstLine, primaryKeyColumnSql, otherColumnSqlList,
                tableCommentSql, columnComments);

        return fullSql;
    }

    private static String generateFullSql(String createTableFirstLine, String primaryKeyColumnSql,
                                          List<String> otherColumnSqlList,
                                          String tableCommentSql, List<String> columnComments) {
        StringBuilder builder = new StringBuilder();
        // 建表语句首行
        builder.append(createTableFirstLine)
                .append("\n");
        // 主键 须缩进
        builder.append("    ")
                .append(primaryKeyColumnSql)
                .append(",\n");

        // 每一列 缩进
        for (int i = 0; i < otherColumnSqlList.size(); i++) {
            if (i != otherColumnSqlList.size() - 1) {
                builder.append("    ").append(otherColumnSqlList.get(i)).append(",\n");
            } else {
                builder.append("    ").append(otherColumnSqlList.get(i)).append("\n");
            }
        }
        builder.append(");\n");

        // 表的注释
        if (tableCommentSql != null) {
            builder.append("\n" + tableCommentSql + "\n");
        }

        // 列的注释
        for (String columnComment : columnComments) {
            builder.append(columnComment).append("\n");
        }

        String sql = builder.toString();
        return sql;
    }

    private static List<String> generateOtherColumnSql(List<ColumnDefinition> columnDefinitions, Index primaryKey) {
        String primaryKeyColumnName = primaryKey.getColumnsNames().get(0);

        List<ColumnDefinition> columnDefinitionList = columnDefinitions.stream()
                .filter((ColumnDefinition column) -> !Objects.equals(column.getColumnName(), primaryKeyColumnName))
                .collect(Collectors.toList());

        List<String> sqlList = new ArrayList<String>();
        for (ColumnDefinition columnDefinition : columnDefinitionList) {
            // 列名
            String columnName = columnDefinition.getColumnName();

            // 类型
            String dataType = columnDefinition.getColDataType().getDataType();
            String postgreDataType = DataTypeMapping.MYSQL_TYPE_TO_POSTGRE_TYPE.get(dataType);
            if (postgreDataType == null) {
                System.out.println(columnDefinition.getColDataType().getArgumentsStringList());
                throw new UnsupportedOperationException("mysql dataType not supported yet. " + dataType);
            }
            // 获取类型后的参数，如varchar(512)中，将获取到512
            List<String> argumentsStringList = columnDefinition.getColDataType().getArgumentsStringList();
            String argument = null;
            if (argumentsStringList != null && argumentsStringList.size() != 0) {
                if (argumentsStringList.size() == 1) {
                    argument = argumentsStringList.get(0);
                } else if (argumentsStringList.size() == 2) {
                    argument = argumentsStringList.get(0) + "," + argumentsStringList.get(1);
                }
            }
            if (argument != null && argument.trim().length() != 0) {
                if (postgreDataType.equalsIgnoreCase("bigint")
                        || postgreDataType.equalsIgnoreCase("smallint")
                        || postgreDataType.equalsIgnoreCase("int")
                ) {
                    postgreDataType = postgreDataType;
                } else {
                    postgreDataType = postgreDataType + "(" + argument + ")";
                }
            }

            // 处理默认值，将mysql中的默认值转为pg中的默认值，如mysql的CURRENT_TIMESTAMP转为
            List<String> specs = columnDefinition.getColumnSpecs();
            int indexOfDefaultItem = specs.indexOf("DEFAULT");
            if (indexOfDefaultItem != -1){
                String mysqlDefault = specs.get(indexOfDefaultItem + 1);
                // 是字符串的情况下，内容可能是数字，也可能不是
                if (mysqlDefault.startsWith("'") && mysqlDefault.endsWith("'")){
                    mysqlDefault = mysqlDefault.replaceAll("'", "");
                }else {
                    // 不是字符串的话，一般就是mysql中的函数，此时要查找对应的pg函数
                    String postgreDefault = DefaultValueMapping.MYSQL_DEFAULT_TO_POSTGRE_DEFAULT.get(mysqlDefault);
                    if (postgreDefault == null) {
                        throw new UnsupportedOperationException("not supported mysql default:" + mysqlDefault);
                    }
                    specs.set(indexOfDefaultItem + 1, postgreDefault);
                }
            }

            String sourceSpec = String.join(" ", specs);
            String targetSpecAboutNull = null;
            if (sourceSpec.contains("DEFAULT NULL")) {
                targetSpecAboutNull = "NULL";
                sourceSpec = sourceSpec.replaceAll("DEFAULT NULL", "");
            } else if (sourceSpec.contains("NOT NULL")) {
                targetSpecAboutNull = "NOT NULL";
                sourceSpec = sourceSpec.replaceAll("NOT NULL", "");
            }

            // postgre不支持unsigned
            sourceSpec = sourceSpec.replaceAll("unsigned", "");
            // postgre不支持ON UPDATE CURRENT_TIMESTAMP
            sourceSpec = sourceSpec.replaceAll("ON UPDATE CURRENT_TIMESTAMP", "");


            String sql;
            if (sourceSpec.trim().length() != 0) {
                sql = String.format("%s %s %s %s", columnName, postgreDataType, targetSpecAboutNull, sourceSpec.trim());
            } else {
                sql = String.format("%s %s %s", columnName, postgreDataType, targetSpecAboutNull);
            }
            sqlList.add(sql);
        }
        return sqlList;
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static String generatePrimaryKeySql(List<ColumnDefinition> columnDefinitions, Index primaryKey) {
        // 仅支持单列主键，不支持多列联合主键
        String primaryKeyColumnName = primaryKey.getColumnsNames().get(0);

        ColumnDefinition primaryKeyColumnDefinition = columnDefinitions.stream()
                .filter((ColumnDefinition column) -> column.getColumnName().equals(primaryKeyColumnName))
                .findFirst().orElse(null);
        if (primaryKeyColumnDefinition == null) {
            throw new RuntimeException();
        }
        String primaryKeyType = null;
        String dataType = primaryKeyColumnDefinition.getColDataType().getDataType();
        if (Objects.equals("bigint", dataType)) {
            primaryKeyType = "bigserial";
        } else if (Objects.equals("int", dataType)) {
            primaryKeyType = "serial";
        } else if (Objects.equals("varchar", dataType)){
            primaryKeyType = primaryKeyColumnDefinition.getColDataType().toString();
        }

        String sql = String.format("%s %s PRIMARY KEY", primaryKeyColumnName, primaryKeyType);

        return sql;
    }

    private static List<String> extractColumnCommentSql(String tableFullyQualifiedName, List<ColumnDefinition> columnDefinitions) {
        List<String> columnComments = new ArrayList<>();
        columnDefinitions
                .forEach((ColumnDefinition columnDefinition) -> {
                    List<String> columnSpecStrings = columnDefinition.getColumnSpecs();

                    int commentIndex = getCommentIndex(columnSpecStrings);
                    if (commentIndex != -1) {
                        int commentStringIndex = commentIndex + 1;
                        String commentString = columnSpecStrings.get(commentStringIndex);

                        String commentSql = genCommentSql(tableFullyQualifiedName, columnDefinition.getColumnName(), commentString);
                        columnComments.add(commentSql);

                        columnSpecStrings.remove(commentStringIndex);
                        columnSpecStrings.remove(commentIndex);
                    }
                });

        return columnComments;
    }

    private static int getCommentIndex(List<String> columnSpecStrings) {
        for (int i = 0; i < columnSpecStrings.size(); i++) {
            if ("COMMENT".equalsIgnoreCase(columnSpecStrings.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private static String genCommentSql(String table, String column, String commentValue) {
        return String.format("COMMENT ON COLUMN %s.%s IS %s;", table, column, commentValue);
    }
}

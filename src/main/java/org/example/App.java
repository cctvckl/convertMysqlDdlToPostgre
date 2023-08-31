package org.example;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Hello world!
 */
public class App {
    public static final HashMap<String, String> MYSQL_TYPE_TO_POSTGRE_TYPE
            = new HashMap<String, String>();

    static {
        MYSQL_TYPE_TO_POSTGRE_TYPE.put("bigint", "bigint");
        MYSQL_TYPE_TO_POSTGRE_TYPE.put("int", "int");
        MYSQL_TYPE_TO_POSTGRE_TYPE.put("varchar", "varchar");
        MYSQL_TYPE_TO_POSTGRE_TYPE.put("datetime", "timestamp");
        MYSQL_TYPE_TO_POSTGRE_TYPE.put("tinyint", "SMALLINT");
    }

    public static String SQL = "DROP TABLE IF EXISTS `my_favorite_url_jump`;\n" +
            "CREATE TABLE `public`.`api_domain_config` (\n" +
            "  `id` bigint(21) NOT NULL AUTO_INCREMENT COMMENT 'id',\n" +
            "  `domain_url` varchar(512) DEFAULT NULL COMMENT '域名url',\n" +
            "  `domain_status` tinyint(4) DEFAULT NULL COMMENT '域名状态,0:正常，1:禁用',\n" +
            "  `created_at` datetime DEFAULT NULL COMMENT '创建时间',\n" +
            "  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',\n" +
            "  `created_by` varchar(32) DEFAULT NULL COMMENT '创建用户名',\n" +
            "  `updated_by` varchar(32) DEFAULT NULL COMMENT '更新用户名',\n" +
            "  `deleted_at` bigint(20) unsigned DEFAULT '0' COMMENT '逻辑删除标志,0为正常,删除时请设置删除时的时间戳到本字段',\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='域名配置'\n";

    public static void main(String[] args) throws JSQLParserException {
        SQL = SQL.replaceAll("`","");
        CreateTable createTable = (CreateTable) CCJSqlParserUtil.parse(SQL);
        String tableFullyQualifiedName = createTable.getTable().getFullyQualifiedName();
        List<ColumnDefinition> columnDefinitions = createTable.getColumnDefinitions();

        /**
         * 生成目标sql：表注释
         */
        List<String> tableOptionsStrings = createTable.getTableOptionsStrings();
        String tableCommentSql = null;
        int commentIndex = tableOptionsStrings.indexOf("COMMENT");
        if (commentIndex != -1){
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

        System.out.println(fullSql);
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
        if (tableCommentSql != null){
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
            String postgreDataType = MYSQL_TYPE_TO_POSTGRE_TYPE.get(dataType);
            // 获取类型后的参数，如varchar(512)中，将获取到512
            List<String> argumentsStringList = columnDefinition.getColDataType().getArgumentsStringList();
            String argument = null;
            if (argumentsStringList != null && argumentsStringList.size() != 0) {
                argument = argumentsStringList.get(0);
            }
            if (argument != null && argument.trim().length() != 0) {
                if (postgreDataType.equalsIgnoreCase("bigint")
                        || postgreDataType.equalsIgnoreCase("smallint")) {
                    postgreDataType = postgreDataType;
                } else {
                    postgreDataType = postgreDataType + "(" + argument + ")";
                }
            }

            // 其他选项
            List<String> specs = columnDefinition.getColumnSpecs();
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
            String sql;
            if (sourceSpec.trim().length() != 0) {
                sql = String.format("%s %s %s %s", columnName, postgreDataType, targetSpecAboutNull, sourceSpec);
            } else {
                sql = String.format("%s %s %s", columnName, postgreDataType, targetSpecAboutNull);
            }
            sqlList.add(sql);
        }
        return sqlList;
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

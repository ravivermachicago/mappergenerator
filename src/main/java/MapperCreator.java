import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Ravi Verma
 *
 */
public class MapperCreator {

    private static Logger logger = LoggerFactory.getLogger(MapperCreator.class);
    private static String tableName;
    //generated classes will got to outputfolder
    private File outputfolder;

    private ClassDefinitionShell pojoShell;
    private ClassDefinitionShell mapperShell;
    private StringBuilder sqlString = new StringBuilder();
    private StringBuilder sqlSgtringEnd = new StringBuilder();

    private static final String queryForMetadataTable = "select * from %s where 1 <> 2";

    private static final String isql = "private sqlstring = \"INSERT INTO %s (";


    private static final String SEMICOLON = ";";
    private static final String NEWLINE = System.getProperty("line.separator");

    private static final String MEMBERVARIABLE = "\tprivate %s %s " + SEMICOLON + NEWLINE;
    private static final String SETTERS = "public void set%s(%s formalParamType){" + NEWLINE + "\t this.%s=formalParamType" + SEMICOLON + NEWLINE + "\t}" + NEWLINE;
    private static final String GETTERS = "public %s get%s(){" + NEWLINE + "\t return this.%s;" + NEWLINE + "\t" + "}" + NEWLINE;

    /***
      when populated NULLCHECK looks like if (mc.getColumnName()==null){ ps.setNull(1,java.lang.Intger); } // assuming ColumnName datatype is int
     ***/
    private final static String NULLCHECK = "if (mc.get%s()==null){" + NEWLINE + "\tps.setNull(%d,%s)" + SEMICOLON + NEWLINE + "}" + NEWLINE;

    /***
     when populated ELSEASSIGN  looks like else { ps.setInt(1,mc.getColumnName);} // assuming ColumnName datatype is int

    ***/
    private final static String ELSEASSIGN = "else {" + NEWLINE + "\tps.%s(%d,mc.get%s())" + SEMICOLON + NEWLINE + "}" + NEWLINE;



    @Autowired
    private DataSource dataSource;


    // methodMap will take  java class name and get its corresponding setter from Statement.class
    Map<String, Method> methodMap = new HashMap<>();

    /**
     classToprimitive is the type of argument for the table that will correspond to the type of datatype to use for setting the column value
     key is  java class, value is type primitive, Statement methods are named to look like they use primitives
    **/
    Map<Class, String> classToprimitive = new HashMap();

    public MapperCreator() {

    }

    public MapperCreator(File outputfolder){
        this.outputfolder=outputfolder;
        initDataTypesFromClassToParameterTypes();
    }

    /**
     * (non-Javadoc)
     *  these are the steps that this method performs
     *  -- get meta data for table using @link {@link ResultSet#getMetaData()}
     *  -- in cache create/append to actual implementation  class of spring that extends @{@link org.springframework.jdbc.core.ParameterizedPreparedStatementSetter}
     *  -- in cache create/append to POJO what forms the parameter to @{@link org.springframework.jdbc.core.ParameterizedPreparedStatementSetter#setValues(PreparedStatement, Object)}   -- for each columm found , cache the information about datatype of column
     *                         cache corresponding type of java.sql.Types for generating null in the setter method.
     *
     * @param tableName from the database
     * @throws SQLException if something is wrong.(wrong table, connection params etc
     */
    public void createClasses(String tableName) throws SQLException {

        this.tableName =tableName;
        JdbcTemplate template = new JdbcTemplate(dataSource);// This is my datasource


        cacheMethodsWhichAreSettersForStatementClass();


        //tablenameDataClass
          pojoShell = new ClassDefinitionShell(String.format("public class %sDataClass {", MapperCreator.tableName),String.format("%sDataClass.java", MapperCreator.tableName));

        //PreparedStatementBuilder
        getStringBuilderWithInvariatentsForMethodSetterForPreparedStatement();

        //create actual sql insert string

        sqlString.append(String.format(isql, MapperCreator.tableName)).append("(");

        Statement stmt;
        ResultSet rs = null;
        ResultSetMetaData rsmtadta;
        int colCount;
        try (Connection con = dataSource.getConnection()) {

            try (Statement statement = stmt = con.createStatement()) {


                rs = stmt.executeQuery(String.format(queryForMetadataTable, MapperCreator.tableName));
                // Get the ResultSet from the query
                rsmtadta = rs.getMetaData();
                colCount = rsmtadta.getColumnCount();
                // Find number of columns in TABLE
                createMetaDataForBeanClass(rsmtadta, colCount);

               logger.info(sqlString.append(sqlSgtringEnd.toString()).toString() + "\";");


                produceClasses();

            } finally {

                rs.close();
            }
        }

    }

    private void produceClasses() {
        GetterSetterContainer gs = new GetterSetterContainer(outputfolder,pojoShell);
        gs.writeClassFile();

        mapperShell.addClosingParen("\n}");
        gs = new GetterSetterContainer(outputfolder,mapperShell);
        gs.writeClassFile();
    }

    private void initDataTypesFromClassToParameterTypes() {

        this.classToprimitive.put(Integer.class, "int");
        this.classToprimitive.put(Long.class, "long");
        this.classToprimitive.put(String.class, "java.lang.String");
        this.classToprimitive.put(Timestamp.class, "java.sql.Timestamp");
        this.classToprimitive.put(Double.class, "double");
        this.classToprimitive.put(Date.class, "java.sql.Date");
        this.classToprimitive.put(java.math.BigDecimal.class, "java.math.BigDecimal");
    }


    private void getStringBuilderWithInvariatentsForMethodSetterForPreparedStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.sql.PreparedStatement;\n" +
                "import java.sql.SQLException;\n" +
                "import java.sql.Types;\n");

        sb.append(String.format("public class %sMapper implements org.springframework.jdbc.core.ParameterizedPreparedStatementSetter<%sDataClass>  {", tableName, tableName)).append('\n');
        sb.append("@Override").append('\n');
        sb.append(String.format("public void setValues(PreparedStatement ps, %sDataClass mc) throws SQLException {", tableName)).append('\n');

        mapperShell = new ClassDefinitionShell(sb.toString(),String.format("%sMapper.java", MapperCreator.tableName));

    }

    private void createMetaDataForBeanClass(ResultSetMetaData rsmtadta, int colCount) throws SQLException {
        for (int i = 1; i <= colCount; i++) {
            String colName = rsmtadta.getColumnName(i);
            String colType = rsmtadta.getColumnClassName(i);
            // Get column data type
            logger.info("Column = " + colName + " is data type " + colType);
            String[] names = colType.split(Pattern.quote("."));
            // Print the column value
            sqlString.append(colName);
            sqlSgtringEnd.append("?");
            logger.info(Arrays.toString(names));
            String typesCol;
            typesCol = getRightTypesData(names[2]);

            appendCommaOrClosingBracket(colCount, i);


            pojoShell.addClassMember(String.format(MEMBERVARIABLE, colType, colName.toLowerCase()));
            String customizedColumnName = colName.charAt(0) + colName.substring(1).toLowerCase();

            pojoShell.addSetterMethods(String.format(SETTERS, customizedColumnName, colType, customizedColumnName.toLowerCase()));

            pojoShell.addGetterMethods(String.format(GETTERS, colType, customizedColumnName, customizedColumnName.toLowerCase()));

            appendToSetterData(i, colName, colType, typesCol);


        }
    }


    /**
     * this is a a very kloogy and a little brittle , ideally all of this can be kept inside of a public interface as this is a static information,
     * but this requires doing more investigation.
     *
     * @param name like java.lang.String and return
     * @return Types value as Types.VARCHAR
     */
    private String getRightTypesData(String name) {
        String typesCol;
        if(name.trim().toUpperCase().equals("LONG")){
             typesCol="Types.BIGINT";
        }else
        if(name.trim().toUpperCase().equals("STRING")){
            typesCol="Types.VARCHAR";
        }
        else
            {
             typesCol = "Types." + name.toUpperCase().trim();
        }
        return typesCol;
    }

    private void appendCommaOrClosingBracket( int colCount, int i) {
        if (i == colCount) {
            sqlString.append(") VALUES ");
            sqlSgtringEnd.append(")");

        } else {
            sqlString.append(",");
            sqlSgtringEnd.append(",");
        }
    }

    private void cacheMethodsWhichAreSettersForStatementClass() {
        ArrayList<Method> methodArrayList = findSetters(PreparedStatement.class);
        for (Method m : methodArrayList) {

            if (m.getParameterCount() == 2) {
                methodMap.put(m.getGenericParameterTypes()[1].getTypeName(), m);
            }

        }
    }

    private void appendToSetterData( int parameterIndexNumber, String colName, String colType,String typesCol) {
        String customizedColumnName = colName.charAt(0)+colName.substring(1).toLowerCase();

        mapperShell.addSetterMethods(String.format(NULLCHECK, customizedColumnName, parameterIndexNumber, typesCol));
        try {
            // getMethodNameInStringFormToUse,WithTypeParameter
            // this will be like setInt or setTimeStamp

            Class javaClassForDataBaseColumn = this.getClass().getClassLoader().loadClass(colType);
            String methodName = getSetterMethodNameFromStatementClass(javaClassForDataBaseColumn);

            mapperShell.addSetterMethods(String.format(ELSEASSIGN, methodName, parameterIndexNumber, customizedColumnName));

        } catch (ClassNotFoundException e) {

            e.printStackTrace();
        }
    }

    private String getSetterMethodNameFromStatementClass(Class javaClassForDataBaseColumn) {
        return methodMap.get(classToprimitive.get(javaClassForDataBaseColumn)).getName();
    }

    static ArrayList<Method> findSetters(Class<?> c) {
        ArrayList<Method> list = new ArrayList<Method>();
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods)
            if (isSetter(method))
                list.add(method);
        return list;
    }


    public static boolean isSetter(Method method) {
        return Modifier.isPublic(method.getModifiers()) &&
                method.getReturnType().equals(void.class) &&
                method.getParameterTypes().length == 2 &&
                method.getName().matches("^set[A-Z].*");
    }


}
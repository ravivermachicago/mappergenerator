public class ClassDefinitionShell {

    private StringBuilder classDefinition=new StringBuilder();
    private StringBuilder classMember=new StringBuilder();
    private StringBuilder setterGetter = new StringBuilder();
    private StringBuilder imports = new StringBuilder();
    private static final String END = "}";
    private  String name;
    private StringBuilder closingparen = new StringBuilder();

    public  void setName(String name) {
        this.name = name;

    }

    public ClassDefinitionShell(String defnition, String javaFileName){
        this.classDefinition=new StringBuilder();
        this.classDefinition.append(defnition);
        this.name = javaFileName;
    }

    public ClassDefinitionShell(String defnition){
        this.classDefinition=new StringBuilder();
        this.classDefinition.append(defnition);

    }

    public void addClassMember(String str){
        this.classMember.append(str);
    }

    public void addSetterMethods(String str){
        this.setterGetter.append(str);
    }

    public void addImport(String str){
        this.imports.append(str);
    }

    public String getJavaClassAsString(){
        return imports.toString()+classDefinition.toString()+classMember.toString()+setterGetter.toString()+closingparen+END;
    }

    public  String getName() {
        return name;
    }


    public void addClosingParen(String s) {
        this.closingparen.append(s);
    }

    public void addGetterMethods(String str) {
        this.setterGetter.append(str);
    }
}

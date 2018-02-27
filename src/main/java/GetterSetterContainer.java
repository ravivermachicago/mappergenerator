
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class GetterSetterContainer {

    private final File outputfolder;

    private ClassDefinitionShell classDefinitionShell;

    public GetterSetterContainer(File outputfolder,ClassDefinitionShell classDefinitionShell) {

        this.outputfolder= outputfolder;
        this.classDefinitionShell = classDefinitionShell;
    }

    public void writeClassFile(){


        BufferedWriter writer = null;
        try {
            //create a temporary file

            File logFile = new File(outputfolder.getAbsolutePath()+System.getProperty("file.separator")+classDefinitionShell.getName());

            // This will output the full path where the file will be written to...
            System.out.println(logFile.getCanonicalPath());

            writer = new BufferedWriter(new FileWriter(logFile));
          //  writer.write(privateMembers.toString());
          //  writer.write(gettersAndSetters.toString());
            writer.write(classDefinitionShell.getJavaClassAsString());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (Exception e) {
            }
        }
    }




}

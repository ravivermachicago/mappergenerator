import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import java.sql.SQLException;

public class CreateCodeClass {


    public static void main(String[] args) throws SQLException {

        if (args.length !=2) {
            System.err.println("Usage CreateCodeClass --tablename=TABLENAME --outputdir==dir");
            System.exit(-1);
        }

        CommandLinePropertySource<?> cmdLinePropertySource = getCommandLinePropertySource(args);
        AnnotationConfigApplicationContext ctx = addCmdLineParmsAndGetContext(cmdLinePropertySource);
        MapperCreator tserToValuableItemMapper = (MapperCreator)ctx.getBean(MapperCreator.class);


        tserToValuableItemMapper.createClasses(cmdLinePropertySource.getProperty("tablename"));

    }

    private static AnnotationConfigApplicationContext addCmdLineParmsAndGetContext(CommandLinePropertySource<?> cmdLinePropertySource) {

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

        ctx.register(DbconnectConfig.class);
        ctx.getEnvironment().getPropertySources().addFirst(cmdLinePropertySource);
        ctx.refresh();


        return ctx;
    }

    private static CommandLinePropertySource<?> getCommandLinePropertySource(String[] args) {

        String[] cmdlingArgs = new String[2];

        cmdlingArgs[0] = args[0];
        cmdlingArgs[1] = args[1];

        return new SimpleCommandLinePropertySource("folderAndClass", cmdlingArgs);
    }
}

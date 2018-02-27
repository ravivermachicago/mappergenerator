import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.io.File;


public class DbconnectConfig {

    @Autowired
    Environment environment;

    @Bean
    public DataSource dataSource()


    {

        PGSimpleDataSource ds = new PGSimpleDataSource();  // Empty instance.
        ds.setServerName("localhost");  // The value `localhost` means the Postgres cluster running locally on the same machine.
        ds.setDatabaseName("sampledatabase");   // A connection to Postgres must be made to a specific database rather than to the server as a whole. You likely have an initial database created named `public`.
        ds.setUser("postgres");         // Or use the super-user 'postgres' for user name if you installed Postgres with defaults and have not yet created user(s) for your application.
        ds.setPassword("postgres");     // You would not


        return ds;
    }


    @Bean
    MapperCreator getmapperCreator() {
        String folderName= environment.getProperty("outputdir");
        File folder = new File(folderName);
        if(folder.exists()==false){
            throw new RuntimeException(String.format("outputfolder %s does not exist ",folderName));
        }

        return new MapperCreator(folder);
    }
}

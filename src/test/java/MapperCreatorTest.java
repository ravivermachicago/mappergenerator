import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.sql.SQLException;

import static org.junit.Assert.assertNotNull;

public class MapperCreatorTest {
    private ApplicationContext ctx ;
    @Before
    public void setUp(){
        ctx = new AnnotationConfigApplicationContext(DbconnectConfig.class);

    }

    @Test
    public void printInfo() throws SQLException {

        MapperCreator tserToValuableItemMapper = (MapperCreator)ctx.getBean(MapperCreator.class);
        assertNotNull(tserToValuableItemMapper);

        tserToValuableItemMapper.createClasses("");
    }
}
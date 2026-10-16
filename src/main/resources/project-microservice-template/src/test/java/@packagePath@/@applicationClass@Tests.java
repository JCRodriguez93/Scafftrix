package @packageName@;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto Spring arranca de verdad: Flyway aplica las migraciones y
 * Hibernate valida que las entidades encajen con el esquema.
 */
@SpringBootTest
@ActiveProfiles("test")
class @applicationClass@Tests {

    @Test
    void contextLoads() {
    }
}

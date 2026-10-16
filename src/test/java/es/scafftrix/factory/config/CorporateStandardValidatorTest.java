package es.scafftrix.factory.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El validador se reescribio cuando JHipster salio de la factoria: antes comprobaba
 * valores del vocabulario del CLI ({@code databaseType=h2Disk}, {@code clientFramework=no},
 * {@code buildTool=maven}, {@code serverPort=8091}), que solo tenian sentido para el
 * generador que ya no se usa.
 *
 * <p>Este test fija el contrato actual: con una configuracion por defecto valida, y solo
 * se rompe si falta el workspace o la concurrencia es invalida.
 */
class CorporateStandardValidatorTest {

    private static ScafftrixProperties defaults() {
        ScafftrixProperties properties = new ScafftrixProperties();
        properties.getWorkspace().setPath("./target/workspace-validator-test");
        properties.getGeneration().setConcurrency(1);
        return properties;
    }

    @Test
    void acceptsTheDefaultConfiguration() {
        assertDoesNotThrow(() -> new CorporateStandardValidator(defaults()).validate());
    }

    @Test
    void rejectsAnEmptyWorkspacePath() {
        ScafftrixProperties properties = defaults();
        properties.getWorkspace().setPath("  ");
        CorporateStandardValidator validator = new CorporateStandardValidator(properties);

        IllegalStateException failure = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, validator::validate);

        assertTrue(failure.getMessage().contains("workspace.path"),
                "El mensaje debe nombrar la propiedad: " + failure.getMessage());
    }

    @Test
    void rejectsZeroConcurrency() {
        ScafftrixProperties properties = defaults();
        properties.getGeneration().setConcurrency(0);
        CorporateStandardValidator validator = new CorporateStandardValidator(properties);

        IllegalStateException failure = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, validator::validate);

        assertTrue(failure.getMessage().contains("concurrency"),
                "El mensaje debe nombrar la propiedad: " + failure.getMessage());
    }

    /**
     * El validador se ejecuta en un contexto real: si le faltara alguna dependencia tras
     * retirar JHipster, aqui lo detectaria antes que en el arranque del contenedor.
     */
    @Test
    void theValidatorIsInstantiableBySpring() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ScafftrixProperties.class, CorporateStandardValidatorTest::defaults);
            context.register(CorporateStandardValidator.class);
            context.refresh();
            assertNotNull(context.getBean(CorporateStandardValidator.class));
        }
    }
}
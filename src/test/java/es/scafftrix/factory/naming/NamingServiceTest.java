package es.scafftrix.factory.naming;

import es.scafftrix.factory.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamingServiceTest {

    private final NamingService namingService = new NamingService();

    @Test
    void deriveBaseNameFromHumanName() {
        assertEquals("gestionDeClientes", namingService.deriveBaseName("Gestión de Clientes"));
        assertEquals("customerManagement", namingService.deriveBaseName("Customer Management"));
    }

    @Test
    void deriveArtifactIdFromHumanName() {
        assertEquals("gestion-de-clientes", namingService.deriveArtifactId("Gestión de Clientes"));
        assertEquals("customer-management", namingService.deriveArtifactId("Customer Management"));
    }

    @Test
    void derivePackageName() {
        assertEquals("es.scafftrix.customermanagement", namingService.derivePackageName("es.scafftrix", "customerManagement"));
        assertEquals("es.scafftrix.gestiondeclientes", namingService.derivePackageName("es.scafftrix", "gestionDeClientes"));
    }

    @Test
    void rejectReservedJavaKeywordInPackage() {
        assertThrows(InvalidConfigurationException.class, () -> namingService.derivePackageName("es.scafftrix", "class"));
        assertThrows(InvalidConfigurationException.class, () -> namingService.validatePackageName("es.scafftrix.int"));
    }

    @Test
    void rejectInvalidGroupId() {
        assertThrows(InvalidConfigurationException.class, () -> namingService.validateGroupId("com/demo"));
        assertThrows(InvalidConfigurationException.class, () -> namingService.validateGroupId("1es.scafftrix"));
    }

    @Test
    void rejectPathTraversalPatterns() {
        assertThrows(InvalidConfigurationException.class, () -> namingService.validateArtifactId("../../etc"));
        assertThrows(InvalidConfigurationException.class, () -> namingService.validatePackageName("es.scafftrix..app"));
    }
}
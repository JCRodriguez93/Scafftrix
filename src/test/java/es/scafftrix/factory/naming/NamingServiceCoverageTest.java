package es.scafftrix.factory.naming;

import es.scafftrix.factory.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NamingServiceCoverageTest {

    private final NamingService namingService = new NamingService();

    @Test
    void sanitizeRejectsNullBlankAndTextWithoutLettersOrDigits() {
        assertThatThrownBy(() -> namingService.sanitizeToWords(null))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.sanitizeToWords("   "))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.sanitizeToWords("@#$ %^&"))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void sanitizeFoldsAccentsAndKeepsDigits() {
        assertThat(namingService.sanitizeToWords("Gestión 2 de Clientes")).isEqualTo("gestion 2 de clientes");
    }

    @Test
    void baseNameIsTruncatedAndStrippedOfTrailingDigits() {
        String candidate = "a".repeat(39) + "1" + "bbbbb";

        assertThat(namingService.deriveBaseName(candidate)).isEqualTo("a".repeat(39));
    }

    @Test
    void artifactIdIsTruncatedAndStrippedOfTrailingHyphens() {
        String candidate = "a".repeat(62) + " b";

        assertThat(namingService.deriveArtifactId(candidate)).isEqualTo("a".repeat(62));
    }

    @Test
    void validatesAndNormalizesAGroupId() {
        assertThat(namingService.validateAndNormalizeGroupId("  es.Scafftrix  ")).isEqualTo("es.Scafftrix");
        assertThat(namingService.validateGroupId("es.scafftrix")).isEqualTo("es.scafftrix");
        assertThat(namingService.derivePackageName("es.Scafftrix", "customerManagement"))
                .isEqualTo("es.scafftrix.customermanagement");
    }

    @Test
    void rejectsAnOverlongGroupId() {
        String tooLong = "a".repeat(121);

        assertThatThrownBy(() -> namingService.validateAndNormalizeGroupId(tooLong))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsAMissingGroupId() {
        assertThatThrownBy(() -> namingService.validateAndNormalizeGroupId(null))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validateAndNormalizeGroupId("   "))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void validatesBaseNameArtifactIdAndVersion() {
        assertThat(namingService.validateBaseName("customerManagement")).isEqualTo("customerManagement");
        assertThat(namingService.validateArtifactId("customer-management")).isEqualTo("customer-management");
        assertThat(namingService.validateVersion(" 1.0.0-SNAPSHOT ")).isEqualTo("1.0.0-SNAPSHOT");

        assertThatThrownBy(() -> namingService.validateBaseName(null))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validateBaseName("Upper"))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validateArtifactId("  "))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validateArtifactId("Upper"))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validateArtifactId("trailing-"))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validateVersion(null))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validateVersion("1 0"))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsBaseNameAndArtifactIdThatAreTooLong() {
        String tooLongBaseName = "a".repeat(41);
        String tooLongArtifactId = "a".repeat(64);

        assertThatThrownBy(() -> namingService.validateBaseName(tooLongBaseName))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validateArtifactId(tooLongArtifactId))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void rejectsInvalidPackageShapes() {
        assertThatThrownBy(() -> namingService.validatePackageName(null))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validatePackageName("single"))
                .isInstanceOf(InvalidConfigurationException.class);
        String tooLongPackage = "es.scafftrix." + "a".repeat(200);

        assertThatThrownBy(() -> namingService.validatePackageName(tooLongPackage))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validatePackageName("java.util"))
                .isInstanceOf(InvalidConfigurationException.class);
        assertThatThrownBy(() -> namingService.validatePackageName("es.scafftrix.9bad"))
                .isInstanceOf(InvalidConfigurationException.class);
    }

    @Test
    void offendingValuesAreSanitisedAndTruncatedInTheTechnicalMessage() {
        InvalidConfigurationException longValue = catchInvalid(() -> namingService.validateArtifactId("Z".repeat(200)));
        assertThat(longValue.getTechnicalMessage()).contains("...");

        InvalidConfigurationException controlChars =
                catchInvalid(() -> namingService.validateArtifactId("bad\u0001value"));
        assertThat(controlChars.getTechnicalMessage()).doesNotContain("\u0001");
    }

    private InvalidConfigurationException catchInvalid(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected an InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            return e;
        }
    }
}

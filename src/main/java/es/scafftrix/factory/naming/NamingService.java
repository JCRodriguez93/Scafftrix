package es.scafftrix.factory.naming;

import es.scafftrix.factory.exception.InvalidConfigurationException;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Derives and validates the technical identifiers used by the generated project.
 *
 * <p>This class is the security boundary for anything that will be written into a
 * file, a package declaration or a generator argument. It applies a strict
 * allow-list: a value is either produced by this service from sanitised text, or it
 * matches an explicitly enumerated pattern. Everything else is rejected.
 *
 * <p>Because identifiers are validated with {@link Pattern#matches(String, CharSequence)}
 * (full-string match, not {@code find}), characters such as {@code ;}, {@code &},
 * {@code |}, backticks, spaces, quotes or path separators can never survive.
 */
@Service
public class NamingService {

    /** Java package segment: starts with a letter, then letters/digits. */
    private static final Pattern PACKAGE_SEGMENT = Pattern.compile("[a-z][a-z0-9]*");

    /** Maven group id: dot separated identifier segments, each starting with a letter. */
    private static final Pattern GROUP_ID =
            Pattern.compile("[A-Za-z]\\w*+(\\.[A-Za-z]\\w++)*+");

    /** Maven artifact id: kebab-case, must start with a letter. */
    private static final Pattern ARTIFACT_ID = Pattern.compile("[a-z][a-z0-9-]*");

    /** JHipster base name: camelCase, must start with a lowercase letter. */
    private static final Pattern BASE_NAME = Pattern.compile("[a-z][a-zA-Z0-9]*");

    /** Project version, restricted to characters used by Maven versions. */
    private static final Pattern VERSION = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._-]*");

    private static final int MAX_BASE_NAME_LENGTH = 40;
    private static final int MAX_ARTIFACT_ID_LENGTH = 63;
    private static final int MAX_PACKAGE_LENGTH = 180;
    private static final int MAX_GROUP_ID_LENGTH = 120;

    /** Shared tail of the "too long" validation messages. */
    private static final String CHARS_SUFFIX = " caracteres.";

    /** Java keywords and literals that can never be used as a package segment. */
    private static final Set<String> RESERVED_SEGMENTS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "var", "record", "yield", "sealed", "permits");

    /** Package roots that must not be used by application code. */
    private static final Set<String> RESERVED_ROOTS = Set.of("java", "javax", "jdk", "sun", "com.sun", "org.w3c", "org.xml");

    /**
     * Normalises free user text into ASCII lowercase words.
     *
     * <p>Accents are folded ({@code Gestión} becomes {@code gestion}) and any character
     * that is not a letter or digit becomes a word separator.
     */
    public String sanitizeToWords(String input) {
        if (input == null || input.isBlank()) {
            throw invalid(input, "El nombre de la aplicación no puede estar vacío.");
        }
        // NFD + strip combining marks folds accents (á -> a).
        String ascii = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String[] rawWords = ascii.split("[^A-Za-z0-9]+");
        List<String> words = new java.util.ArrayList<>();
        for (String word : rawWords) {
            if (!word.isBlank()) {
                words.add(word);
            }
        }
        if (words.isEmpty()) {
            throw invalid(input, "El nombre de la aplicación debe contener al menos una letra o un dígito.");
        }
        return String.join(" ", words).toLowerCase(Locale.ROOT);
    }

    /** Builds a camelCase JHipster base name from free text, e.g. {@code customerManagement}. */
    public String deriveBaseName(String applicationName) {
        String words = sanitizeToWords(applicationName);
        StringBuilder camel = new StringBuilder();
        for (String word : words.split(" ")) {
            if (camel.isEmpty()) {
                camel.append(word);
            } else {
                camel.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        String candidate = camel.toString();
        candidate = truncateBaseName(candidate);
        assertBaseName(candidate, applicationName);
        return candidate;
    }

    /** Builds a kebab-case artifact id from free text, e.g. {@code customer-management}. */
    public String deriveArtifactId(String applicationName) {
        String words = sanitizeToWords(applicationName);
        String candidate = words.replace(" ", "-");
        if (candidate.length() > MAX_ARTIFACT_ID_LENGTH) {
            candidate = candidate.substring(0, MAX_ARTIFACT_ID_LENGTH);
            // Do not leave trailing hyphens after truncation.
            candidate = stripTrailing(candidate, '-');
        }
        assertArtifactId(candidate, applicationName);
        return candidate;
    }

    /**
     * Appends the base name to the group id to form the Java package,
     * e.g. {@code es.scafftrix} + {@code customerManagement} becomes
     * {@code es.scafftrix.customermanagement}.
     */
    public String derivePackageName(String groupId, String baseName) {
        // Java packages are conventionally lowercase; Maven group ids may contain uppercase.
        String normalizedGroup = validateAndNormalizeGroupId(groupId).toLowerCase(Locale.ROOT);
        String normalizedBase = validateBaseName(baseName);
        String packageName = normalizedGroup + "." + normalizedBase.toLowerCase(Locale.ROOT);
        assertPackageName(packageName, groupId + "/" + baseName);
        return packageName;
    }

    public String validateAndNormalizeGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw invalid(groupId, "El Group ID es obligatorio.");
        }
        String candidate = groupId.trim();
        if (candidate.length() > MAX_GROUP_ID_LENGTH) {
            throw invalid(candidate, "El Group ID no puede superar " + MAX_GROUP_ID_LENGTH + CHARS_SUFFIX);
        }
        if (!GROUP_ID.matcher(candidate).matches()) {
            throw invalid(candidate,
                    "El Group ID no es válido. Debe tener el formato es.scafftrix (segmentos separados por puntos, "
                            + "empezando por una letra).");
        }
        return candidate;
    }

    public String validateGroupId(String groupId) {
        validateAndNormalizeGroupId(groupId);
        return groupId.trim();
    }

    public String validateBaseName(String baseName) {
        assertBaseName(baseName, baseName);
        return baseName;
    }

    public String validateArtifactId(String artifactId) {
        assertArtifactId(artifactId, artifactId);
        return artifactId;
    }

    public String validatePackageName(String packageName) {
        assertPackageName(packageName, packageName);
        return packageName;
    }

    public String validateVersion(String version) {
        if (version == null || version.isBlank()) {
            throw invalid(version, "La versión del proyecto es obligatoria.");
        }
        String candidate = version.trim();
        if (!VERSION.matcher(candidate).matches()) {
            throw invalid(candidate, "La versión no es válida. Ejemplos válidos: 1.0.0, 1.0.0-SNAPSHOT, 2.1");
        }
        return candidate;
    }

    /** Strips a trailing digit left behind by truncating a camelCase name. */
    private String truncateBaseName(String candidate) {
        if (candidate.length() <= MAX_BASE_NAME_LENGTH) {
            return candidate;
        }
        String truncated = candidate.substring(0, MAX_BASE_NAME_LENGTH);
        return stripTrailingDigits(truncated);
    }

    /** Removes every trailing occurrence of {@code toStrip}, without using a backtracking regex. */
    private static String stripTrailing(String value, char toStrip) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == toStrip) {
            end--;
        }
        return value.substring(0, end);
    }

    /** Removes every trailing digit, e.g. the {@code 1} left on {@code customerManagement1}. */
    private static String stripTrailingDigits(String value) {
        int end = value.length();
        while (end > 0 && Character.isDigit(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private void assertBaseName(String baseName, String originalInput) {
        if (baseName == null || baseName.isBlank()) {
            throw invalid(originalInput, "El base name no puede estar vacío.");
        }
        if (baseName.length() > MAX_BASE_NAME_LENGTH) {
            throw invalid(baseName, "El base name no puede superar " + MAX_BASE_NAME_LENGTH + CHARS_SUFFIX);
        }
        if (!BASE_NAME.matcher(baseName).matches()) {
            throw invalid(baseName,
                    "El base name debe ser camelCase y empezar por una letra minúscula, por ejemplo customerManagement.");
        }
    }

    private void assertArtifactId(String artifactId, String originalInput) {
        if (artifactId == null || artifactId.isBlank()) {
            throw invalid(originalInput, "El Artifact ID no puede estar vacío.");
        }
        if (artifactId.length() > MAX_ARTIFACT_ID_LENGTH) {
            throw invalid(artifactId, "El Artifact ID no puede superar " + MAX_ARTIFACT_ID_LENGTH + CHARS_SUFFIX);
        }
        if (!ARTIFACT_ID.matcher(artifactId).matches()) {
            throw invalid(artifactId,
                    "El Artifact ID debe estar en kebab-case y empezar por una letra minúscula, "
                            + "por ejemplo customer-management.");
        }
        if (artifactId.endsWith("-")) {
            throw invalid(artifactId, "El Artifact ID no puede terminar en guion.");
        }
    }

    private void assertPackageName(String packageName, String originalInput) {
        if (packageName == null || packageName.isBlank()) {
            throw invalid(originalInput, "El package name no puede estar vacío.");
        }
        if (packageName.length() > MAX_PACKAGE_LENGTH) {
            throw invalid(packageName, "El package name es demasiado largo.");
        }
        String[] segments = packageName.split("\\.", -1);
        if (segments.length < 2) {
            throw invalid(packageName, "El package name debe tener al menos dos segmentos, por ejemplo es.scafftrix.app.");
        }
        Set<String> normalized = new LinkedHashSet<>(Arrays.asList(segments));
        for (String segment : segments) {
            if (!PACKAGE_SEGMENT.matcher(segment).matches()) {
                throw invalid(packageName,
                        "El package name contiene un segmento inválido: '" + segment
                                + "'. Use sólo minúsculas, letras y dígitos, empezando por una letra.");
            }
            if (RESERVED_SEGMENTS.contains(segment)) {
                throw invalid(packageName,
                        "El package name no puede usar la palabra reservada de Java '" + segment + "'.");
            }
        }
        for (String root : RESERVED_ROOTS) {
            if (normalized.contains(root) || packageName.startsWith(root + ".")) {
                throw invalid(packageName,
                        "El package name no puede empezar por el espacio reservado '" + root + "'.");
            }
        }
    }

    private InvalidConfigurationException invalid(String input, String userMessage) {
        String safeInput = input == null ? "null" : input.replaceAll("[\\p{Cntrl}]", "?");
        if (safeInput.length() > 120) {
            safeInput = safeInput.substring(0, 120) + "...";
        }
        return new InvalidConfigurationException(userMessage,
                "Rejected invalid project identifier. userMessage=" + userMessage + " offendingValue='" + safeInput + "'");
    }
}

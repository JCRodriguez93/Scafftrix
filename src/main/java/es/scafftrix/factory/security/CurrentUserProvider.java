package es.scafftrix.factory.security;

import java.util.Locale;

/**
 * Identity of the developer using the portal.
 *
 * <p>The MVP runs unauthenticated behind the corporate network perimeter, but every
 * created project is already attributed to a principal so that adding LDAP, Active
 * Directory, OAuth2 or OIDC later is a matter of registering a different
 * implementation of this interface: no controller, service or entity has to change.
 */
public interface CurrentUserProvider {

    /**
     * @return the current principal
     * @throws org.springframework.security.core.AuthenticationCredentialsNotFoundException
     *         when no authenticated principal exists
     */
    CurrentUser currentUser();

    /**
     * A simple, immutable user record.
     *
     * @param username login name, used for attribution
     * @param displayName human readable name shown in the UI
     */
    record CurrentUser(String username, String displayName) {

        public CurrentUser {
            if (username == null || username.isBlank()) {
                throw new IllegalArgumentException("username must not be blank");
            }
            username = username.trim().toLowerCase(Locale.ROOT);
            if (displayName == null || displayName.isBlank()) {
                displayName = username;
            }
        }
    }
}
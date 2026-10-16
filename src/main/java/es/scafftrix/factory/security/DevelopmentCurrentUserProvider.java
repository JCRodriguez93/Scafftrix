package es.scafftrix.factory.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default provider used while the portal runs without an identity provider.
 *
 * <p>It reports a fixed development user so that project attribution works from day one.
 * A real implementation backed by Spring Security's {@code Authentication} for
 * LDAP/AD/OIDC can replace this class when authentication is introduced.
 */
@Component
public class DevelopmentCurrentUserProvider implements CurrentUserProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DevelopmentCurrentUserProvider.class);

    private static final String USERNAME = "developer";
    private static final String DISPLAY_NAME = "Developer (SSO not configured)";

    @Override
    public CurrentUser currentUser() {
        return new CurrentUser(USERNAME, DISPLAY_NAME);
    }

    public DevelopmentCurrentUserProvider() {
        LOG.warn("No CurrentUserProvider found: running with the built-in development identity '{}'. "
                + "Configure LDAP/AD/OIDC before exposing this portal outside the corporate network.", USERNAME);
    }
}

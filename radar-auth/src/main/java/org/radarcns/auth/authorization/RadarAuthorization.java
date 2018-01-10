package org.radarcns.auth.authorization;

import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.radarcns.auth.exception.NotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authorization helper class for RADAR. This class checks if the authenticated user is allowed to
 * access the protected resources of a given subject based on the authorities and project
 * affiliations.
 */
public class RadarAuthorization {

    private static final Logger log = LoggerFactory.getLogger(RadarAuthorization.class);
    public static final String AUTHORITIES_CLAIM = "authorities";
    public static final String ROLES_CLAIM = "roles";
    public static final String SCOPE_CLAIM = "scope";
    public static final String SOURCES_CLAIM = "sources";
    public static final String GRANT_TYPE_CLAIM = "grant_type";
    private static final String CLIENT_CREDENTIALS = "client_credentials";

    /**
     * Check if the user authenticated with the given token has the given permission. Not taking
     * into account project affiliations. Throws a {@link NotAuthorizedException} if the supplied
     * token does not have the permission.
     * @param token The token of the logged in user
     * @param permission The permission to check
     * @throws NotAuthorizedException if the supplied token does not have the permission
     */
    public static void checkPermission(DecodedJWT token, Permission permission)
            throws NotAuthorizedException {
        log.debug("Checking permission {} for user {}", permission.toString(), token.getSubject());
        checkScope(token, permission);
        if (!isClientCredentials(token)) {
            // it's not a client_credentials token, so we need both scope and authority
            checkPermission(token, permission, getAuthorities(token));
        }
    }

    /**
     * Check if the user authenticated with the given token has the given permission in a project.
     * Throws a {@link NotAuthorizedException} if the supplied token does not have the permission
     * in the given project.
     * @param token The token of the logged in user
     * @param permission The permission to check
     * @param projectName The project for which to check the permission
     * @throws NotAuthorizedException if the supplied token does not have the permission in the
     *     given project
     */
    public static void checkPermissionOnProject(DecodedJWT token, Permission permission,
            String projectName) throws NotAuthorizedException {
        log.debug("Checking permission {} for user {} in project {}", permission.toString(),
                token.getSubject(), projectName);
        checkScope(token, permission);
        if (!isClientCredentials(token)) {
            checkPermission(token, permission, getAuthoritiesForProject(token, projectName));
        }
    }

    /**
     * Check if the user authenticated with the given token has the given permission on a specific
     * subject in a project. Throws a {@link NotAuthorizedException} if the supplied token does
     * not have the permission in the given project for the given subject.
     * @param token The token of the logged in user
     * @param permission The permission to check
     * @param projectName The project for which to check the permission
     * @param subjectName The name of the subject to check
     * @throws NotAuthorizedException if the supplied token does not have the permission in the
     *     given project for the given subject
     */
    public static void checkPermissionOnSubject(DecodedJWT token, Permission permission,
            String projectName, String subjectName) throws NotAuthorizedException {
        log.debug("Checking permission {} for user {} on subject {} in project {}",
                permission.toString(), token.getSubject(), subjectName, projectName);
        checkScope(token, permission);
        if (!isClientCredentials(token)) {
            // we're allowed to read our own data
            if (token.getSubject().equals(subjectName) && Permissions.allowedAuthorities(permission)
                    .contains(AuthoritiesConstants.PARTICIPANT)) {
                return;
            }

            // if we're only a participant, and we're not the subject we request data for,
            // we don't have access
            if (isJustParticipant(token, projectName)) {
                throw new NotAuthorizedException(String.format("User %s does not have permission"
                        + " %s in project %s for subject %s", token.getSubject(),
                        permission.toString(), projectName, subjectName));
            } else {
                // otherwise we have other roles and we should check on a project level
                checkPermission(token, permission, getAuthoritiesForProject(token, projectName));
            }
        }
    }

    /**
     * Check if this user is just a participant in the project.
     * @param token Token of the authenticated user
     * @param projectName Project to check
     * @return true if PARTICIPANT is the only authority of the user in the project, false otherwise
     */
    public static boolean isJustParticipant(DecodedJWT token, String projectName) {
        if (!token.getClaims().containsKey(ROLES_CLAIM)) {
            return false;
        }
        List<String> roles = token.getClaim(ROLES_CLAIM).asList(String.class).stream()
                .filter(r -> r.startsWith(projectName + ":"))
                .collect(Collectors.toList());
        return roles.size() == 1 && roles.get(0).equals(projectName + ":"
                + AuthoritiesConstants.PARTICIPANT);
    }

    private static Set<String> getAuthoritiesForProject(DecodedJWT token, String projectName) {
        // get all project-based authorities
        Set<String> result = token.getClaims().containsKey(ROLES_CLAIM)
                ? token.getClaim(ROLES_CLAIM).asList(String.class).stream()
                        .filter(s -> s.startsWith(projectName + ":"))
                        .map(s -> s.split(":")[1])
                        .collect(Collectors.toSet())
                : new HashSet<>();
        // also add SYS_ADMIN authority if we have it
        if (token.getClaims().containsKey(AUTHORITIES_CLAIM)
                && token.getClaim(AUTHORITIES_CLAIM).asList(String.class)
                .contains(AuthoritiesConstants.SYS_ADMIN)) {
            result.add(AuthoritiesConstants.SYS_ADMIN);
        }
        return result;
    }

    private static boolean hasScope(DecodedJWT token, Permission permission) {
        return token.getClaim(SCOPE_CLAIM).asList(String.class).contains(permission.scopeName());
    }

    protected static void checkPermission(DecodedJWT token, Permission permission,
            Set<String> authsGranted) throws NotAuthorizedException {
        // Take intersection of both sets
        authsGranted.retainAll(Permissions.allowedAuthorities(permission));
        if (authsGranted.isEmpty()) {
            log.info("User {} does not have permission {}", token.getSubject(),
                    permission.toString());
            throw new NotAuthorizedException(String.format("User %s does not have permission %s",
                    token.getSubject(), permission.toString()));
        }
    }

    private static Set<String> getAuthorities(DecodedJWT token) {
        Set<String> result = new HashSet<>();
        // get all project-based authorities
        if (token.getClaims().containsKey(ROLES_CLAIM)) {
            result.addAll(token.getClaim(ROLES_CLAIM).asList(String.class).stream().filter(s -> s
                    .contains(":"))
                    .map(s -> s.split(":")[1]).collect(Collectors.toSet()));
        }
        // also add non-project based authorities
        if (token.getClaims().containsKey(AUTHORITIES_CLAIM)) {
            result.addAll(token.getClaim(AUTHORITIES_CLAIM).asList(String.class));
        }
        return result;
    }

    private static void checkScope(DecodedJWT token, Permission permission)
            throws NotAuthorizedException {
        if (hasScope(token, permission)) {
            return;
        } else {
            throw new NotAuthorizedException(String.format("Client %s does not have "
                    + "permission %s", token.getSubject(), permission.toString()));
        }
    }

    private static boolean isClientCredentials(DecodedJWT token) {
        return CLIENT_CREDENTIALS.equals(token.getClaim(GRANT_TYPE_CLAIM).asString());
    }
}

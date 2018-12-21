package de.sb.radio.rest;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Priority;
import javax.persistence.EntityManager;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import de.sb.radio.persistence.HashTools;
import de.sb.radio.persistence.Person;
import de.sb.toolbox.Copyright;
import de.sb.toolbox.net.HttpCredentials;
import de.sb.toolbox.net.RestCredentials;
import de.sb.toolbox.net.RestJpaLifecycleProvider;


/**
 * JAX-RS filter provider that performs HTTP "basic" authentication on any REST
 * service request. This aspect-oriented design swaps "Authorization" headers
 * for "Requester-Identity" during authentication.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@Copyright(year = 2017, holders = "Sascha Baumeister")
public class BasicAuthenticationFilter implements ContainerRequestFilter {

	/**
	 * HTTP request header for the authenticated requester's identity.
	 */
	static public final String REQUESTER_IDENTITY = "Requester-Identity";


	/**
	 * Performs HTTP "basic" authentication by calculating a password hash from
	 * the password contained in the request's "Authorization" header, and
	 * comparing it to the one stored in the person matching said header's
	 * username. The "Authorization" header is consumed in any case, and upon
	 * success replaced by a new "Requester-Identity" header that contains the
	 * authenticated person's identity. The filter chain is aborted in case of a
	 * problem.
	 * 
	 * @param requestContext
	 *            {@inheritDoc}
	 * @throws NullPointerException
	 *             if the given argument is {@code null}
	 * @throws BadRequestException
	 *             if the "Authorization" header is malformed, or if there is a
	 *             pre-existing "Requester-Identity" header
	 */
	public void filter (final ContainerRequestContext requestContext) throws NullPointerException, BadRequestException, NotAuthorizedException {
		if (requestContext.getHeaders().containsKey(REQUESTER_IDENTITY))
			throw new BadRequestException();
		final List<String> header = requestContext.getHeaders().remove(AUTHORIZATION);
		final String textCredentials = header == null || header.isEmpty() ? null : header.get(0);

		if (textCredentials != null) {
			final HttpCredentials.Basic credentials = RestCredentials.newBasicInstance(textCredentials);

			final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
			final List<Person> people = radioManager.createQuery("select p from Person as p where p.email = :email", Person.class).setParameter("email", credentials.getUsername()).getResultList();

			if (people.size() == 1) {
				final Person person = people.get(0);
				final byte[] leftHash = person.getPasswordHash();
				final byte[] rightHash = HashTools.sha256HashCode(credentials.getPassword());

				if (Arrays.equals(leftHash, rightHash)) {
					requestContext.getHeaders().add(REQUESTER_IDENTITY, Long.toString(person.getIdentity()));
					return;
				}
			}
		}

		requestContext.abortWith(Response.status(UNAUTHORIZED).header(WWW_AUTHENTICATE, "Basic").build());
	}
}
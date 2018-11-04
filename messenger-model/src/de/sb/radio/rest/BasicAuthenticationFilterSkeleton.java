package de.sb.radio.rest;

import javax.annotation.Priority;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import de.sb.toolbox.Copyright;


/**
 * JAX-RS filter provider that performs HTTP "basic" authentication on any REST service request. This aspect-oriented
 * design swaps "Authorization" headers for "Requester-Identity" during authentication.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
@Copyright(year = 2017, holders = "Sascha Baumeister")
public class BasicAuthenticationFilterSkeleton implements ContainerRequestFilter {

	/**
	 * HTTP request header for the authenticated requester's identity.
	 */
	static public final String REQUESTER_IDENTITY = "Requester-Identity";


	/**
	 * Performs HTTP "basic" authentication by calculating a password hash from the password contained in the request's
	 * "Authorization" header, and comparing it to the one stored in the person matching said header's username. The
	 * "Authorization" header is consumed in any case, and upon success replaced by a new "Requester-Identity" header that
	 * contains the authenticated person's identity.
	 * @param requestContext {@inheritDoc}
	 * @throws NullPointerException if the given argument is {@code null}
	 * @throws BadRequestException if the "Authorization" header is malformed, or if there is a pre-existing
	 *         "Requester-Identity" header
	 * @throws NotAuthorizedException if the "Authorization" header is absent, not the "basic" type, or not authentic
	 */
	public void filter (final ContainerRequestContext requestContext) throws NullPointerException, BadRequestException, NotAuthorizedException {
		// TODO:
		// - Throw a BadRequestException if the given context's headers map already contains a
		//   "Requester-Identity" key, in order to prevent spoofing attacks.
		// - Remove the "Authorization" header from said map and store the first of it's values in a variable
		//   "textCredentials", or null if the header value is either null or empty.
		// - if the "textCredentials" variable is not null, parse it either programmatically, or using
		//   RestCredentials.newBasicInstance(textCredentials).
		// - Perform the PQL-Query "select p from Person as p where p.email = :email"), using the username
		//   of the parsed credentials as email address. Note that this query will go to the second level cache
		//   before hitting the database if the Person#email field is annotated using @CacheIndex(updateable = true)! 
		// - if the resulting people list contains exactly one element, calculate the SHA-256 hash code of
		//   the credential's password either programmatically, or using HashTools.sha256HashCode().
		// - if Arrays.equals() says this hash code equals the the queried person's password hash,
		//   add a new "Requester-Identity" header to the request headers, using the person's identity
		//   (converted to String) as value, and return from this method.
		// - in all other cases, abort the request in order to challenging the client to provide HTTP Basic credentials
		//   (i.e. status code 401, and "WWW-Authenticate" header value "Basic"). Note that the alternative
		//   (throw new NotAuthorizedException("Basic")) comes with the disadvantage that failed authentication attempts
		//   clutter the server log with stack traces.
	}
}
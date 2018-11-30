package de.sb.radio.rest;

import static de.sb.radio.persistence.Person.Group.*;
import static de.sb.radio.rest.BasicAuthenticationFilter.REQUESTER_IDENTITY;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.persistence.TypedQuery;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import de.sb.radio.persistence.Album;
import de.sb.radio.persistence.BaseEntity;
import de.sb.radio.persistence.Document;
import de.sb.radio.persistence.HashTools;
import de.sb.radio.persistence.Person;
import de.sb.radio.persistence.Track;
import de.sb.toolbox.Copyright;
import de.sb.toolbox.net.RestJpaLifecycleProvider;
import de.sb.toolbox.val.NotEqual;


/**
 * JAX-RS based REST service implementation for polymorphic entity resources,
 * defining the following path and method combinations:
 * <ul>
 * <li>GET entities/{id}: Returns the entity matching the given identity.</li>
 * <li>DELETE entities/{id}: Deletes the entity matching the given
 * identity.</li>
 * </ul>
 */

// Die Klasse bietet Service-Methoden für alle Arten
// von Entitäten, ist also polymorph

@Path("")
@Copyright(year = 2013, holders = "Sascha Baumeister")
public class EntityService {

	static private final Set<String> EMPTY_WORD_SINGLETON = Collections.singleton("");
	static private final Set<Byte> EMPTY_BYTE_SINGLETON = Collections.singleton(Byte.valueOf((byte) -1));
	static private final String QUERY_DOCUMENT_BY_HASH = "select d.identity from Document as d where d.contentHash = :contentHash";
	static private final String QUERY_ALBUMS = "select a.identity from Album as a where " 
			+ "((:title is null) or (a.title = :title)) and " 
			+ "((:releaseYear is null) or (a.releaseYear = :releaseYear)) and "
			+ "((:trackCount is null) or (a.trackCount = :trackCount))";
	static private final String QUERY_TRACKS = "select t.identity from Track as t where " 
			+ "((:name is null) or (t.name = :name)) and " 
			+ "((:artist is null) or (t.artist = :artist)) and "
			+ "((:ignoreGenres = true) or (t.genre in :genres)) and "
			+ "((:ignoreOrdinals = true) or (t.ordinal in :ordinals))";
	static private final String QUERY_PEOPLE = "select p.identity from Person p where " 
			+ "((:surname is null) or (p.surname = :surname)) and " 
			+ "((:forename is null) or (p.forename = :forename)) and " 
			+ "((:email is null) or (p.email = :email))";

	static private final String QUERY_GENRES = "select distinct t.genre from Track as t";
	static private final String QUERY_ARTISTS = "select distinct t.artist from Track as t";


	/**
	 * Returns the entity with the given identity.
	 * 
	 * @param entityIdentity
	 *            the entity identity
	 * @return the matching entity (HTTP 200)
	 * @throws ClientErrorException
	 *             (HTTP 404) if the given entity cannot be found
	 * @throws PersistenceException
	 *             (HTTP 500) if there is a problem with the persistence layer
	 * @throws IllegalStateException
	 *             (HTTP 500) if the entity manager associated with the current
	 *             thread is not open
	 */
	@GET
	@Path("entities/{id}")
	@Produces({ APPLICATION_JSON, APPLICATION_XML })
	public BaseEntity queryEntity (@PathParam("id") @Positive final long entityIdentity) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final BaseEntity entity = radioManager.find(BaseEntity.class, entityIdentity);
		if (entity == null)
			throw new ClientErrorException(NOT_FOUND);

		return entity;
	}


	/**
	 * Deletes the entity matching the given identity, or does nothing if no
	 * such entity exists.
	 * 
	 * @param requesterIdentity
	 *            the authenticated requester identity
	 * @param entityIdentity
	 *            the entity identity
	 * @return void (HTTP 204)
	 * @throws ClientErrorException
	 *             (HTTP 403) if the given requester is not an administrator
	 * @throws ClientErrorException
	 *             (HTTP 404) if the given entity cannot be found
	 * @throws ClientErrorException
	 *             (HTTP 409) if there is a database constraint violation (like
	 *             conflicting locks)
	 * @throws PersistenceException
	 *             (HTTP 500) if there is a problem with the persistence layer
	 * @throws IllegalStateException
	 *             (HTTP 500) if the entity manager associated with the current
	 *             thread is not open
	 */
	@DELETE
	@Path("entities/{id}")
	public void deleteEntity (@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity, @PathParam("id") @Positive final long entityIdentity) {

		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final Person requester = radioManager.find(Person.class, requesterIdentity);
		if (requester == null || requester.getGroup() != ADMIN)
			throw new ClientErrorException(FORBIDDEN);

		// TODO: check if getReference() works once
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=460063 is fixed.
		final BaseEntity entity = radioManager.find(BaseEntity.class, entityIdentity);
		if (entity == null)
			throw new ClientErrorException(NOT_FOUND);
		radioManager.remove(entity);

		try {
			radioManager.getTransaction().commit();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			radioManager.getTransaction().begin();
		}

		radioManager.getEntityManagerFactory().getCache().evict(BaseEntity.class, entityIdentity);
	}


	/**
	 * GET /people: Returns the people matching the given filter criteria, with
	 * missing parameters identifying omitted criteria, sorted by family name,
	 * given name, email. Search criteria should be any “normal” property of
	 * person and it’s composites, except identity and password, plus
	 * resultOffset and resultLimit which define a result range.
	 */
	@GET
	@Path("people")
	@Produces(APPLICATION_JSON)
	public List<Person> returnPeople (@QueryParam("surname") String surname, @QueryParam("forename") String forename, @QueryParam("email") String email) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		
		final TypedQuery<Long> query = radioManager.createQuery(QUERY_PEOPLE, Long.class);		
		query.setParameter("surname", surname);
		query.setParameter("forename", forename);
		query.setParameter("email", email);

		final List<Long> personReferences = query.getResultList();
		final List<Person> people = new ArrayList<>();
		for (final long id : personReferences) {
			final Person person = radioManager.find(Person.class, id);
			if (person != null)
				people.add(person);
		}
		people.sort(Comparator.comparing(Person::getSurname).thenComparing(Person::getForename).thenComparing(Person::getEmail));
		
		//List<Person> allPersons = query.getResultList().stream().sorted(Comparator.comparing(Person::getSurname).thenComparing(Person::getForename).thenComparing(Person::getEmail)).collect(Collectors.toList());

		return people;
	}


	@POST
	@Path("people")
	@Consumes(APPLICATION_JSON)
	@Produces(TEXT_PLAIN)
	public long createOrModifyPerson (@NotNull @Valid Person personTemplate, @HeaderParam(REQUESTER_IDENTITY) @PositiveOrZero final long requesterIdentity, @HeaderParam("Set-Password") final String password,
			@QueryParam("avatarReference") final Long avatarReference) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final Person requester = radioManager.find(Person.class, requesterIdentity);

		if (requester.getGroup() != ADMIN) {
			if (requesterIdentity != personTemplate.getIdentity()) {
				throw new ClientErrorException(FORBIDDEN);
			}
		}

		final boolean insert = personTemplate.getIdentity() == 0;

		final Person person;

		if (insert) { // neue Person erstellen mit default Avatar
			final Document defaultAvatar = radioManager.find(Document.class, 1L);
			if (defaultAvatar == null)
				throw new IllegalStateException(); // ErrorCode 500
			person = new Person(defaultAvatar);
		} else { // Person, die bereits in DB existiert updaten
			person = radioManager.find(Person.class, personTemplate.getIdentity()); // Person
																					// aus
																					// DB
																					// suchen
			if (person == null)
				throw new ClientErrorException(Status.NOT_FOUND); // code 404 //
																	// Not FOund
		}

		if (personTemplate.getGroup() != USER && person.getGroup() == USER)
			throw new ClientErrorException(FORBIDDEN);

		if (avatarReference != null) {
			final Document avatar = radioManager.find(Document.class, avatarReference);
			if (avatar == null)
				throw new ClientErrorException(Status.NOT_FOUND);
			person.setAvatar(avatar);
		}

		person.setGroup(personTemplate.getGroup());
		person.setForename(personTemplate.getForename());
		person.setSurname(personTemplate.getSurname());
		person.setEmail(personTemplate.getEmail());

		// person.setAvatar(personTemplate.getAvatar());
		if (password != null)
			person.setPasswordHash(HashTools.sha256HashCode(password));

		// 3. Schritt

		if (insert) {
			radioManager.persist(person);
		} else {
			radioManager.flush(); // für alle veränderten Objekte im 1st Level
									// Cache
		}

		try {
			radioManager.getTransaction().commit();
		} catch (PersistenceException error) {
			throw new ClientErrorException(Status.CONFLICT);
		} finally {
			radioManager.getTransaction().begin();
		}
		return person.getIdentity();
	}


	@GET
	@Path("people/{id}")
	@Produces(APPLICATION_JSON)
	public Person queryPersonId (@PathParam("id") @PositiveOrZero final long personIdentity, @HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final long identity = personIdentity == 0 ? requesterIdentity : personIdentity;
		final Person person = radioManager.find(Person.class, identity);
		if (person == null)
			throw new ClientErrorException(Status.NOT_FOUND);
		return person;
	}


	@GET
	@Path("albums")
	@Produces(APPLICATION_JSON)
	public Collection<Album> queryAlbums (@QueryParam("resultOffset") int resultOffset, @QueryParam("resultLimit") int resultLimit, @QueryParam("title") String title, @QueryParam("releaseYear") Short releaseYear,
			@QueryParam("trackCount") Byte trackCount) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final TypedQuery<Long> query = radioManager.createQuery(QUERY_ALBUMS, Long.class);
		if (resultOffset > 0)
			query.setFirstResult(resultOffset);
		if (resultLimit > 0)
			query.setMaxResults(resultLimit);
		query.setParameter("title", title);
		query.setParameter("releaseYear", releaseYear);
		query.setParameter("trackCount", trackCount);

		final List<Long> albumReferences = query.getResultList();
		final List<Album> albums = new ArrayList<>();
		for (final long id : albumReferences) {
			final Album album = radioManager.find(Album.class, id);
			if (album != null)
				albums.add(album);
		}
		albums.sort(Comparator.comparing(Album::getTitle).thenComparing(Album::getIdentity));
		return albums;
	}


	@POST
	@Path("albums")
	@Consumes(APPLICATION_JSON)
	@Produces(TEXT_PLAIN)
	public long createOrModifyAlbum (
			@NotNull @Valid Album albumTemplate, 
			@HeaderParam(REQUESTER_IDENTITY) @PositiveOrZero final long requesterIdentity, 
			@QueryParam("coverReference") final Long coverReference
	) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final Person requester = radioManager.find(Person.class, requesterIdentity);

		if (requester == null || requester.getGroup() != ADMIN)
			throw new ClientErrorException(FORBIDDEN);

		final boolean insert = albumTemplate.getIdentity() == 0;
		final Album album;

		if (insert) {
			if (coverReference == null) throw new IllegalStateException();
			album = new Album(null);
		} else {
			album = radioManager.find(Album.class, albumTemplate.getIdentity());
			if (album == null) throw new ClientErrorException(Status.NOT_FOUND);
		}

		album.setReleaseYear(albumTemplate.getReleaseYear());
		album.setTitle(albumTemplate.getTitle());
		album.setTrackCount(albumTemplate.getTrackCount());
		
		if (coverReference != null) {
			final Document cover = radioManager.find(Document.class, coverReference);
			if (cover == null) throw new ClientErrorException(NOT_FOUND);
			album.setCover(cover);
		}

		if (insert) {
			radioManager.persist(album);
		} else {
			radioManager.flush();
		}

		try {
			radioManager.getTransaction().commit();
		} catch (PersistenceException error) {
			throw new ClientErrorException(Status.CONFLICT);
		} finally {
			radioManager.getTransaction().begin();
		}
		return album.getIdentity();
	}


	@GET
	@Path("tracks")
	@Produces(APPLICATION_JSON)
	public Collection<Track> queryTracks (
			@QueryParam("resultOffset") int resultOffset, 
			@QueryParam("resultLimit") int resultLimit, @QueryParam("name") String name, 
			@QueryParam("artist") String artist, 
			@QueryParam("genre") @NotNull Set<String> genres,
			@QueryParam("ordinal") @NotNull Set<Byte> ordinals
	) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final TypedQuery<Long> query = radioManager.createQuery(QUERY_TRACKS, Long.class);
		
		if (resultOffset > 0)
			query.setFirstResult(resultOffset);
		if (resultLimit > 0)
			query.setMaxResults(resultLimit);
		query.setParameter("name", name);
		query.setParameter("artist", artist);
		query.setParameter("ignoreGenres", genres.isEmpty());
		query.setParameter("genres", genres.isEmpty() ? EMPTY_WORD_SINGLETON : genres);
		query.setParameter("ignoreOrdinals", ordinals.isEmpty());
		query.setParameter("ordinals", ordinals.isEmpty() ? EMPTY_BYTE_SINGLETON : ordinals);

		final List<Long> trackReferences = query.getResultList();
		final List<Track> resultTracks = new ArrayList<>();
		for (final long id : trackReferences) {
			final Track track = radioManager.find(Track.class, id);
			if (track != null)
				resultTracks.add(track);
		}
		resultTracks.sort(Comparator.comparing(Track::getName).thenComparing(Track::getIdentity));
		return resultTracks;
	}


	@POST
	@Path("tracks")
	@Consumes(APPLICATION_JSON)
	@Produces(TEXT_PLAIN)
	public long createOrModifyTracks (
			@NotNull @Valid Track trackTemplate, 
			@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity, 
			@QueryParam("recordingReference") final Long recordingReference,
			@QueryParam("albumReference") final Long albumReference
	) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final Person requester = radioManager.find(Person.class, requesterIdentity);		
		if (requester == null) throw new ClientErrorException(NOT_FOUND);
		if (requester.getGroup() != ADMIN) throw new ClientErrorException(FORBIDDEN);

		final boolean insert = trackTemplate.getIdentity() == 0;
		final Track track;

		if (insert) {
			if (albumReference == null | recordingReference == null) throw new ClientErrorException(NOT_FOUND);
			track = new Track(null, null, requester);
		} else {
			track = radioManager.find(Track.class, trackTemplate.getIdentity());
			if (track == null || track.getOwner().getIdentity() != requester.getIdentity()) throw new ClientErrorException(NOT_FOUND);
		}

		track.setName(trackTemplate.getName());
		track.setArtist(trackTemplate.getArtist());
		track.setGenre(trackTemplate.getGenre());
		track.setOrdinal(trackTemplate.getOrdinal());

		if (recordingReference != null) {
			final Document recording = radioManager.find(Document.class, recordingReference);
			if (recording == null) throw new ClientErrorException(NOT_FOUND);
			track.setRecording(recording);
		}

		if (albumReference != null) {
			final Album album = radioManager.find(Album.class, albumReference);
			if (album == null) throw new ClientErrorException(NOT_FOUND);
			track.setAlbum(album);
		}

		if (insert) {
			radioManager.persist(track);
		} else {
			radioManager.flush();
		}

		try {
			radioManager.getTransaction().commit();
		} catch (PersistenceException error) {
			throw new ClientErrorException(Status.CONFLICT);
		} finally {
			radioManager.getTransaction().begin();
		}

		final Cache cache = radioManager.getEntityManagerFactory().getCache();
		if (albumReference != null) cache.evict(Album.class, albumReference);
		if (insert) cache.evict(Person.class, requesterIdentity);

		return track.getIdentity();
	}


	@GET
	@Path("albums/{id}")
	@Produces(APPLICATION_JSON)
	public Album queryAlbum (@PathParam("id") @Positive final long albumIdentity) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final Album album = radioManager.find(Album.class, albumIdentity);
		if (album == null)
			throw new ClientErrorException(Status.NOT_FOUND);
		return album;
	}


	@GET
	@Path("documents/{id}")
	@Produces(WILDCARD)
	public Response queryDocument (@PathParam("id") final long documentIdentity) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final Document document = radioManager.find(Document.class, documentIdentity);
		if (document == null)
			throw new ClientErrorException(Status.NOT_FOUND);
		return Response.ok(document.getContent(), document.getContentType()).build();
	}


	/**
	 * POST /albums: Creates or updates an album from template data within the
	 * HTTP request body in application/json format. The requesting user must be
	 * an ADMIN, if not this operation shall be forbidden. It creates a new
	 * album if the given template's identity is zero, otherwise it updates the
	 * corresponding album with the given data. Optionally, a new cover
	 * reference may be passed using the query parameter "coverReference".
	 * Returns the affected album's identity as text/plain.
	 * 
	 * @param albumTemplate
	 * @param requesterIdentity
	 * @return
	 */
	@POST
	@Path("documents")
	@Consumes(WILDCARD)
	@Produces(TEXT_PLAIN)
	public long createDocument (
			@NotNull final byte[] content, 
			@HeaderParam("Content-type") @NotNull final String contentType
	) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		
		final TypedQuery<Long> query = radioManager.createQuery(QUERY_DOCUMENT_BY_HASH, Long.class);
		query.setParameter("contentHash", HashTools.sha256HashCode(content));
		
		final List<Long> documentReferences = query.getResultList();
		if (!documentReferences.isEmpty()) return documentReferences.get(0);

		final Document document = new Document();
		document.setContent(content);
		document.setContentType(contentType);
		radioManager.persist(document);

		try {
			radioManager.getTransaction().commit();
		} catch (PersistenceException error) {
			throw new ClientErrorException(Status.CONFLICT);
		} finally {
			radioManager.getTransaction().begin();
		}
		return document.getIdentity();
	}
	
	@GET
	@Path("genres")
	@Produces(APPLICATION_JSON)
	public Collection<String> queryGenres (
	) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final TypedQuery<String> query = radioManager.createQuery(QUERY_GENRES, String.class);

		final List<String> genreResults = query.getResultList();

		genreResults.sort(Comparator.naturalOrder());
		return genreResults;
	}
	
	@GET
	@Path("artists")
	@Produces(APPLICATION_JSON)
	public Collection<String> queryArtists (
	) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		final TypedQuery<String> query = radioManager.createQuery(QUERY_ARTISTS, String.class);

		final List<String> artistResults = query.getResultList();

		artistResults.sort(Comparator.naturalOrder());
		return artistResults;
	}

	// Umwandeln für processed Audio soll audio aufnehmen
	@POST
	@Path("processedAudio")
	@Consumes("audio/*")
	@Produces(TEXT_PLAIN)
	public long createProcessedAudio (
			@NotNull final byte[] content, 
			@HeaderParam("Content-type") @NotNull final String contentType
	) {
		final EntityManager radioManager = RestJpaLifecycleProvider.entityManager("radio");
		
		final TypedQuery<Long> query = radioManager.createQuery(QUERY_DOCUMENT_BY_HASH, Long.class);
		query.setParameter("contentHash", HashTools.sha256HashCode(content));
		
		final List<Long> documentReferences = query.getResultList();
		if (!documentReferences.isEmpty()) return documentReferences.get(0);

		final Document processedAudio = new Document();
		processedAudio.setContent(content);
		processedAudio.setContentType(contentType);
		radioManager.persist(processedAudio);

		try {
			radioManager.getTransaction().commit();
		} catch (PersistenceException error) {
			throw new ClientErrorException(Status.CONFLICT);
		} finally {
			radioManager.getTransaction().begin();
		}
		return processedAudio.getIdentity();
	}
	
	
	/**
	 * Returns the messages caused by the entity matching the given identity, in
	 * natural order.
	 * 
	 * @param entityIdentity
	 *            the entity identity
	 * @return the messages caused by the matching entity (HTTP 200)
	 * @throws ClientErrorException
	 *             (HTTP 404) if the given message cannot be found
	 * @throws PersistenceException
	 *             (HTTP 500) if there is a problem with the persistence layer
	 * @throws IllegalStateException
	 *             (HTTP 500) if the entity manager associated with the current
	 *             thread is not open
	 */

	// @GET
	// @Path("{id}/messagesCaused")
	// @Produces({ APPLICATION_JSON, APPLICATION_XML })
	// public Message[] queryMessagesCaused (
	// @PathParam("id") @Positive final long entityIdentity
	// ) {
	// final EntityManager radioManager =
	// RestJpaLifecycleProvider.entityManager("radio");
	// final BaseEntity entity = radioManager.find(BaseEntity.class,
	// entityIdentity);
	// if (entity == null) throw new ClientErrorException(NOT_FOUND);
	//
	// final Message[] messages = entity.getMessagesCaused().toArray(new
	// Message[0]);
	// Arrays.sort(messages);
	// return messages;
	// }
}
package de.sb.radio.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import de.sb.toolbox.Copyright;
import de.sb.toolbox.net.HttpFileHandler;
import de.sb.toolbox.net.RestCorsHeaderProvider;
import de.sb.toolbox.net.RestJpaLifecycleProvider;
import de.sb.toolbox.net.RestResponseCodeProvider;


/**
 * <p>This fassade is used within a Java-SE VM to programmatically deploy REST services.
 * Programmatic server-startup is solely required in Java-SE, as any Java-EE engine must 
 * ship a built-in HTTP server implementation combined with an XML-based configuration.
 * The server factory class used is Jersey-specific, while the HTTP server class used
 * is Oracle/OpenJDK-specific. There are plenty HTTP server types more suitable for
 * production environments, such as Apache Tomcat, Grizzly, Simple, etc; however, they
 * all require a learning courve for successful configuration, while this design
 * auto-configures itself as long as the package of the service classes matches
 * this class's package.</p>
 * <p>Note that for LAZY fetching of entities within <i>EclipseLink</i> (dynamic weaving),
 * add this to the JVM start parameters: -javaagent:[path]eclipselink.jar</p>
 * <p>Also note that in order to force <i>Firefox</i> to prefer JSON over XML in status bar
 * requests, enter about:config into said bar, look for the <tt>network.http.accept.default</tt>
 * entry, and append it's value with this: <tt>,application/json;q=0.95"</tt>.</p>
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ApplicationContainer {

	/**
	 * Application entry point.
	 * @param args the runtime arguments (service port, resource directory, key store file, key recovery password and key
	 *        management password, all optional)
	 * @throws IllegalArgumentException if the given port is not a number, or if the given directory is not a directory
	 * @throws IOException if there is an I/O related problem
	 * @throws CertificateException if any of the certificates in the key store could not be loaded
	 * @throws UnrecoverableKeyException if there is a key recovery problem, like incorrect passwords
	 * @throws KeyManagementException if there is a key management problem, like key expiration
	 */
	static public void main (final String[] args) throws IllegalArgumentException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
		final int servicePort = args.length > 0 ? Integer.parseInt(args[0]) : 8001;
		final Path resourceDirectory = Paths.get(args.length > 1 ? args[1] : "").toAbsolutePath();
		final Path keyStoreFile = args.length > 2 ? Paths.get(args[2]).toAbsolutePath() : null;
		final String keyRecoveryPassword = args.length > 3 ? args[3] : "changeit";
		final String keyManagementPassword = args.length > 4 ? args[4] : keyRecoveryPassword;
		if (!Files.isDirectory(resourceDirectory)) throw new IllegalArgumentException();

		// Note that Jersey automatically registers it's MOXY feature for both
		// JSON and XML marshaling; however, for XML marshaling make sure that
		// every type marshaled is annotated with @XmlRootElement!
		final ResourceConfig configuration = new ResourceConfig()
			.packages(ApplicationContainer.class.getPackage().toString())
			.register(RestResponseCodeProvider.class)
			.register(RestCorsHeaderProvider.class)
			.register(RestJpaLifecycleProvider.open("radio"));

		// Generate keystore for a given host using this JDK utility (default passwords are "changeit"):
		// keytool -genkey -alias <hostname> -keyalg RSA -validity 365 -keystore keystore.jks
		final SSLContext context = newTLSContext(keyStoreFile, keyRecoveryPassword, keyManagementPassword);
		final URI uri = newServiceURI(servicePort, keyStoreFile != null);

		// Create container for REST service, internal resource (class loader), and external resource (file system) access
		final HttpServer container = JdkHttpServerFactory.createHttpServer(uri, configuration, context);
		final HttpFileHandler internalFileHandler = HttpFileHandler.newInstance("/internal");
		final HttpFileHandler externalFileHandler = HttpFileHandler.newInstance("/external", resourceDirectory);
		container.createContext(internalFileHandler.getContextPath(), internalFileHandler);
		container.createContext(externalFileHandler.getContextPath(), externalFileHandler);

		try {
			final String origin = String.format("%s://%s:%s", uri.getScheme(), uri.getHost(), uri.getPort());
			System.out.format("Web container running on origin %s, enter \"quit\" to stop.\n", origin);
			System.out.format("Context path \"%s\" is configured for REST service access.\n", uri.getPath());
			System.out.format("Context path \"%s\" is configured for class loader access.\n", internalFileHandler.getContextPath());
			System.out.format("Context path \"%s\" is configured for file system access within \"%s\".\n", externalFileHandler.getContextPath(), resourceDirectory);
			System.out.format("Bookmark %s%s/WEB-INF/radio.html for application access.\n", origin, internalFileHandler.getContextPath());
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		} finally {
			container.stop(0);
		}
	}


	/**
	 * Creates a new HTTP/HTTPS service URI.
	 * @param port the local port
	 * @param secure {@code true} for HTTPS, {@code false} for HTTP
	 * @return the service URI created
	 * @throws UnknownHostException if the local host cannot be determined
	 */
	static private URI newServiceURI (final int port, final boolean secure) throws UnknownHostException {
		try {
			final String scheme = secure ? "https" : "http";
			final String hostName = InetAddress.getLocalHost().getCanonicalHostName();
			return new URI(scheme, null, hostName, port, "/services", null, null);
		} catch (final URISyntaxException exception) {
			throw new AssertionError(exception);
		}
	}


	/**
	 * Returns a new SSL context based on a JKS key store and the most recent supported transport layer security (TLS) version.
	 * @param keyStoreFile the key store file path, or {@code null} for none
	 * @param keyRecoveryPassword the key recovery password
	 * @param keyManagementPassword the key management password
	 * @return the SSL context created, or {@code null} if no key store is passed
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IOException if an I/O related problem occurs during key store file access
	 * @throws CertificateException if any of the certificates in the key store could not be loaded
	 * @throws UnrecoverableKeyException if there is a key recovery problem, like incorrect passwords
	 * @throws KeyManagementException if there is a key management problem, like key expiration
	 */
	static private SSLContext newTLSContext (final Path keyStoreFile, final String keyRecoveryPassword, final String keyManagementPassword) throws NullPointerException, IOException, CertificateException, UnrecoverableKeyException, KeyManagementException {
		if (keyStoreFile == null) return null;

		try {
			final KeyStore keyStore = KeyStore.getInstance("JKS");
			try {
				try (InputStream byteSource = Files.newInputStream(keyStoreFile)) {
					keyStore.load(byteSource, keyRecoveryPassword.toCharArray());
				}
			} catch (final IOException exception) {
				if (exception.getCause() instanceof UnrecoverableKeyException) throw (UnrecoverableKeyException) exception.getCause();
				throw exception;
			}

			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, keyManagementPassword.toCharArray());

			final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);

			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
			return sslContext;
		} catch (final NoSuchAlgorithmException | KeyStoreException exception) {
			throw new AssertionError(exception);
		}
	}
}
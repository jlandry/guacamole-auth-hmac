package com.dmwl.guacamole.net.encryptedurl;

import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.net.auth.simple.SimpleAuthenticationProvider;
import org.glyptodon.guacamole.net.auth.simple.SimpleConnection;
import org.glyptodon.guacamole.net.auth.simple.SimpleConnectionDirectory;
import org.glyptodon.guacamole.properties.GuacamoleProperties;
import org.glyptodon.guacamole.properties.IntegerGuacamoleProperty;
import org.glyptodon.guacamole.properties.StringGuacamoleProperty;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dmwl.guacamole.net.encryptedurl.Security;

import javax.servlet.http.HttpServletRequest;
import java.util.*; 

public class EncryptedurlAuthenticationProvider extends SimpleAuthenticationProvider {

	public static final long TEN_MINUTES = 10 * 60 * 1000;

	// Properties file params
	private static final StringGuacamoleProperty SECRET_KEY = new StringGuacamoleProperty() {
		@Override
		public String getName() { return "secret-key"; }
	};

	private static final StringGuacamoleProperty DEFAULT_PROTOCOL = new StringGuacamoleProperty() {
		@Override
		public String getName() { return "default-protocol"; }
	};

	private static final IntegerGuacamoleProperty TIMESTAMP_AGE_LIMIT = new IntegerGuacamoleProperty() {
		@Override
		public String getName() { return "timestamp-age-limit"; }
	};

	private static final Logger logger = LoggerFactory.getLogger(EncryptedurlAuthenticationProvider.class);

	// these will be overridden by properties file if present
	private String defaultProtocol = "rdp";
	private long timestampAgeLimit = TEN_MINUTES; // 10 minutes

	// Per-request params
	public static final String SIGNATURE_PARAM = "signature";
	public static final String ID_PARAM = "id";
	public static final String TIMESTAMP_PARAM = "timestamp";
	public static final String PARAM_PREFIX = "guac.";

	private static final List<String> SIGNED_PARAMETERS = new ArrayList<String>() {{
		add("username");
		add("password");
		add("hostname");
		add("port");
	}};

	private Security security;

	private final TimeProviderInterface timeProvider;

	public EncryptedurlAuthenticationProvider(TimeProviderInterface timeProvider) {
		this.timeProvider = timeProvider;
	}

	public EncryptedurlAuthenticationProvider() {
		timeProvider = new DefaultTimeProvider();
	}

	public String getIdentifier() {
		return "hmac";
	}

	@Override
	public Map<String, GuacamoleConfiguration> getAuthorizedConfigurations(Credentials credentials) throws GuacamoleException {
		if (security == null) {
			initFromProperties();
		}

		GuacamoleConfiguration config = getGuacamoleConfiguration(credentials.getRequest());

		if (config == null) {
			return null;
		}

		Map<String, GuacamoleConfiguration> configs = new HashMap<String, GuacamoleConfiguration>();
		configs.put(config.getParameter("id"), config);
		return configs;
	}

	@Override
	public UserContext updateUserContext(UserContext context, AuthenticatedUser user ) throws GuacamoleException {
		Credentials credentials = user.getCredentials();
		HttpServletRequest request = credentials.getRequest();
		GuacamoleConfiguration config = getGuacamoleConfiguration(request);
		if (config == null) {
			return null;
		}
		String id = config.getParameter("id");
		SimpleConnectionDirectory connections = (SimpleConnectionDirectory) context.getConnectionDirectory();
		SimpleConnection connection = new SimpleConnection(id, id, config);
		connection.setParentIdentifier("ROOT");
		connections.putConnection(connection);

		return context;
	}

	@Override
	public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser authenticatedUser,
			Credentials credentials) throws GuacamoleException {

		return authenticateUser(credentials);
	}
	private GuacamoleConfiguration getGuacamoleConfiguration(HttpServletRequest request) throws GuacamoleException {
		if (security == null) {
			initFromProperties();
		}
		String signature1 = request.getParameter(SIGNATURE_PARAM);

		//Decrypt signature Here!

		String signature = security.decrypt(signature1);

		logger.debug("Get hmac signature: {}", signature);

		if (signature == null) {
			return null;
		}
		signature = signature.replace(' ', '+');

		String timestamp = request.getParameter(TIMESTAMP_PARAM);
		if (!checkTimestamp(timestamp)) {
			return null;
		}

		GuacamoleConfiguration config = parseConfigParams(request);

		// Hostname is required!
		if (config.getParameter("hostname") == null) {
			return null;
		}

		// Hostname is required!
		if (config.getProtocol() == null) {
			return null;
		}

		StringBuilder message = new StringBuilder(timestamp)
				.append(config.getProtocol());

		for (String name : SIGNED_PARAMETERS) {
			String value = config.getParameter(name);
			if (value == null) {
				continue;
			}
			message.append(name);
			message.append(value);
		}

		logger.debug("Get hmac message: {}", message.toString());

		//        if (!signatureVerifier.verifySignature(signature, message.toString())) {
		//            return null;
		//        }
		String id = request.getParameter(ID_PARAM);
		if (id == null) {
			id = "DEFAULT";
		} else {
			// This should really use BasicGuacamoleTunnelServlet's IdentfierType, but it is private!
			// Currently, the only prefixes are both 2 characters in length, but this could become invalid at some point.
			// see: guacamole-client@a0f5ccb:guacamole/src/main/java/org/glyptodon/guacamole/net/basic/BasicGuacamoleTunnelServlet.java:244-252
			id = id.substring(2);
		}
		// This isn't normally part of the config, but it makes it much easier to return a single object
		config.setParameter("id", id);
		return config;
	}

	private boolean checkTimestamp(String ts) {
		if (timestampAgeLimit == 0) {
			return true;
		}

		if (ts == null) {
			return false;
		}

		long timestamp = Long.parseLong(ts, 10);
		long now = timeProvider.currentTimeMillis();
		return timestamp + timestampAgeLimit > now;
	}

	private GuacamoleConfiguration parseConfigParams(HttpServletRequest request) {
		GuacamoleConfiguration config = new GuacamoleConfiguration();

		Map<String, String[]> params = request.getParameterMap();

		for (String name : params.keySet()) {
			String value = request.getParameter(name);
			if (!name.startsWith(PARAM_PREFIX) || value == null || value.length() == 0) {
				continue;
			}
			else if (name.equals(PARAM_PREFIX + "protocol")) {
				config.setProtocol(request.getParameter(name));
			}
			else {
				config.setParameter(name.substring(PARAM_PREFIX.length()), request.getParameter(name));
			}
		}

		if (config.getProtocol() == null) config.setProtocol(defaultProtocol);

		return config;
	}

	private void initFromProperties() throws GuacamoleException {
		String secretKey = GuacamoleProperties.getRequiredProperty(SECRET_KEY);
		security = new Security(secretKey);
		defaultProtocol = GuacamoleProperties.getProperty(DEFAULT_PROTOCOL);
		if (defaultProtocol == null) defaultProtocol = "rdp";
		if (GuacamoleProperties.getProperty(TIMESTAMP_AGE_LIMIT) == null){
			timestampAgeLimit = TEN_MINUTES;
		}  else {
			timestampAgeLimit = GuacamoleProperties.getProperty(TIMESTAMP_AGE_LIMIT);
		}
	}
}

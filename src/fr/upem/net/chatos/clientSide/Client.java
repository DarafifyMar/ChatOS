package fr.upem.net.chatos.clientSide;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.SelectionKey;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import fr.upem.net.chatos.serverSide.ServerChatOS;


public class Client {
	private static final Logger logger = Logger.getLogger(Client.class.getName());
	private final String pseudo;
	private final ClientConnection connection;
	private final Console console = new Console();
	private final LinkedList<String> pendingRequests = new LinkedList<>();
	private final Map<String, ClientConnection> receiverClients = new HashMap<>();
	private final ServerChatOS server;
	private final Map<String, ArrayList<String>> pendingMessages = new HashMap<>();

	public void sendPrivateMessage(String login, String message) {
		request(OPCodes.SEND_PRIVATE, Map.of("receiver", pseudo, "message", message));
	}

	public void requestNewClient(String login) {
		//server.requestNewClient(login);
	}
	
	private Client( String pseudo, ClientConnection connection, ServerChatOS server) throws IOException {
		this.pseudo = Objects.requireNonNull(pseudo);
		this.connection = Objects.requireNonNull(connection);
		this.server = server;
	}

	public static void serve(String host, int port, String home, String pseudo) throws IOException {
		var client = new Client(
			pseudo,
			ClientConnection.startNew(host, port),
			new ServerChatOS(port)
		);
		client.connect();
	}

	private void connect() throws IOException {
		connection.connect(token -> {
			return new ClientContext(token, this::onDisconnect);
		});
		request(OPCodes.SIGN_IN, Map.of("pseudo", pseudo));
	}
	
	private void onDisconnect(SelectionKey serverToken) {
		logger.info("vous avez été déconnecté");
		connection.disconnect();
	}
	
	private void onWrite(String line) {
		if (!line.isBlank() && !line.isEmpty()) {
			line = line.trim();			
		}
		if (!pendingRequests.isEmpty()) {
			sendPrivateRequestAnswer(line);
		} else {
			
		}
	}
	
	void sendPublicMessage(String message) {
		request(OPCodes.PUBLIC_MESSAGE, Map.of("sender", pseudo, "message", message));
	}

	void sendPrivateFile(String line) {
		console.write("fichier envoyé");
	}

	private void sendPrivateMessage(String line) {
		var pattern = Pattern.compile("@(?<login>\\w+)\\s+message\\s+(?<message>.+)");
		var matcher = pattern.matcher(line);
		if (matcher.find()) {
			var login = matcher.group("login");
			var message = matcher.group("message");
			var direct = receiverClients.get(login);
			if (direct != null) {
				direct.request(OPCodes.SEND_PRIVATE, Map.of("sender", this.pseudo, "message", message));
			} else {
				sendPrivateRequest(login,message);
			}
		}
	}
	
	// SERVER -> CLIENT
	
	private void onSignedIn(SelectionKey key, Map<String, Object> params) {
		console.write(pseudo + " vous être connecté au serveur");
	}

	private void onSignedInFailed(SelectionKey key, Map<String, Object> params) {
		console.write("votre connection a échoué");
		connection.disconnect();
	}

	private void onReceiveMessage(SelectionKey key, Map<String, Object> params) {
		var sender = params.get("sender");
		var message = params.get("message");
		console.write(sender+":"+ message);
	}

	private void onRequestPrivate(SelectionKey key, Map<String, Object> params) {
		var sender = (String) params.get("pseudo");
		if (!pendingRequests.contains(sender)) { // ignore duplication			
			pendingRequests.add(sender);
			console.write( sender+" vous a envoyé une demande de communication privée, accepter ? (accepter|refuser)");
		}
	}
	
	private void onOkPrivate(SelectionKey key, Map<String, Object> params) {
		var sender = (String) params.get("pseudo");
		var address = (String) params.get("address");
		console.write(sender+ " a accepté votre demande de communication privée");
		try {
			var connectiontokens = address.split(":");
			var conn = ClientConnection.startNew(connectiontokens[0], Integer.parseInt(connectiontokens[1]));
			receiverClients.put(sender, conn);
			conn.request(OPCodes.SEND_PRIVATE, Map.of("pseudo", pseudo));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private void onNoPrivate(SelectionKey key, Map<String, Object> params) {
		var sender = (String) params.get("pseudo");
		pendingMessages.remove(sender);
		console.write(sender + " a refusé votre demande de communication privée");
	}
	
	private void onConnectPrivate(SelectionKey key, Map<String, Object> params) {
		var sender = (String) params.get("pseudo");
		console.write("Connexion privée établie avec" + sender);
		var messages = pendingMessages.get(sender);
		if (messages != null) {			
			messages.forEach(message -> {
				sendPrivateMessage(message);
			});
			pendingMessages.remove(sender);
		}
	}
	
	private void sendPrivateRequest(String target, String message) {
		var messages = pendingMessages.get(target);
		if (messages == null) {
			messages = new ArrayList<>();
		}
		messages.add(message);
		pendingMessages.put(target, messages);
		request(OPCodes.REQUEST_PRIVATE, Map.of("login", target));
	}

	private void sendPrivateRequestAnswer(String target) {
		request(OPCodes.ACCEPT_PRIVATE, Map.of("login", target));
	}

	private void request(OPCodes code, Map<String, Object> params) {
		connection.request(code, params);
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 4) {
			logger.info("Usage: java fr.upem.net.chatos.clientSide host port directory login");
			return;
		}
		Client.serve(args[0], Integer.parseInt(args[1]), args[2], args[3]);
	}
}

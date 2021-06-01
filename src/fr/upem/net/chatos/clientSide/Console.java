package fr.upem.net.chatos.clientSide;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class Console {
	
	private static final int AUTHORIZED_BUFFER_SIZE = 1024;

	private Client senderClient;
	//private final byte opcode;
	
	private final Map<String,Consumer<String>> commandParser = Map.of(
			"/^@.*/",(Consumer<String>)this::parsePrivateMessage,
			"/^\\.*/", (Consumer<String>)this::parsePrivateFile,
			"/^(?![\\@].*$).*/",(Consumer<String>)this::parsePublicMessage
	);
			
	private void parsePrivateMessage(String command) {
		var pattern = Pattern.compile("@(?<login>\\w+)\\s+message\\s+(?<message>.+)");
		var matcher = pattern.matcher(command);
		if (matcher.find()) {
			var login = matcher.group("login");
			var message = matcher.group("message");
			senderClient.sendPrivateMessage(login, message);
		}
	}
	
	private void parsePrivateFile(String command) {
		senderClient.sendPrivateFile(command);
	}
	
	private void parsePublicMessage(String command) {
		senderClient.sendPublicMessage(command);
	}

	public void run() {
		try (var scanner = new Scanner(System.in)) {
			while (scanner.hasNextLine()) {
				var line = scanner.nextLine();
				if (line.isBlank()) {
					continue;
				}
				
				if (Charset.forName("UTF-8").encode(line).remaining() > AUTHORIZED_BUFFER_SIZE) {
					System.out.println("Line is too long, must be inferior to " + AUTHORIZED_BUFFER_SIZE);
					continue;
				}
				
				Optional<Consumer<String>> parser = commandParser.entrySet().stream()
						  .filter(e -> Pattern.matches(e.getKey(), line))
						  .map(Map.Entry::getValue)
						  .findFirst();
				if(parser.isEmpty()) {
					continue;
				}
				parser.get().accept(line);
			}
		}
	}

	public void write(String format, Object...args) {
		System.out.println(String.format(format, args));
	}

}

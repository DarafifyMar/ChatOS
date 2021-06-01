package fr.upem.net.chatos.clientSide;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import fr.upem.net.chatos.readers.Message;
import fr.upem.net.chatos.readers.PrivateMessageReader;
import fr.upem.net.chatos.readers.PublicMessageReader;

public class ClientContext {

	public static final int BUFFER_SIZE = 1_024;
	private EncodeAndDecode encodeAndDecode = new EncodeAndDecode();
	private final SelectionKey key;
	private final SocketChannel channel;

	private final ByteBuffer in = ByteBuffer.allocate(BUFFER_SIZE);
	private final ByteBuffer out = ByteBuffer.allocate(BUFFER_SIZE);

	private final Consumer<SelectionKey> onClosed;

	private final LinkedList<Consumer<ByteBuffer>> queue = new LinkedList<>();
	
	private boolean closed;

	public ClientContext(SelectionKey token, Consumer<SelectionKey> onClosed) {
		this.key = Objects.requireNonNull(token);
		this.onClosed = Objects.requireNonNull(onClosed);
		this.channel = (SocketChannel) token.channel();
	}

	public void close() {
		try {
			this.onClosed.accept(key);
			channel.close();
		} catch (IOException e) {
			// ignore exception
		}
	}

	public void doRead() throws IOException {
		if (channel.read(in) == -1) {
			this.close();
			return;
		}
		processIn();
		updateInterestOps();
	}

	public void doWrite() throws IOException {
		out.flip();
		channel.write(out);
		out.compact();
		processOut();
		updateInterestOps();
	}

	public void updateInterestOps() {
		var ops = 0;
		if (!closed && in.hasRemaining()) {
			ops |= SelectionKey.OP_READ;
		}
		if (out.position() > 0) {
			ops |= SelectionKey.OP_WRITE;
		}
		if (ops == 0) {
			close();
		} else {
			key.interestOps(ops);
		}
	}

	public void write(Consumer<ByteBuffer> write) {
		queue.add(write);
		processOut();
		updateInterestOps();
	}

	public void write(OPCodes code, Map<String, Object> paramList) {
		var format = code.getFormat();
		LinkedHashMap<String, String> params;
		if (format.isEmpty() || format.isBlank()) {
			params = new LinkedHashMap<>();
		}
		else {
			params = Arrays.stream(format.split(":")).collect(LinkedHashMap::new, (map, e) -> {
			}, Map::putAll);
		}
		 
		queue.add(writer -> {
			writer.put((byte)code.getCode());
			for (var entry : params.entrySet()) {
				var encoder = encodeAndDecode.encodersList.get("string");
					var paramName = entry.getKey();
					var param = paramList.get(paramName);
					if (param == null) {
						throw new IllegalArgumentException(String.format("Paramètre manquant"+ paramName +" dans la commande "+ code.toString()));
					}
					encoder.accept(param, writer);
			}
		});
		processOut();
		updateInterestOps();
	}

	private void processIn() {
		read(key, in);
	}
	public void read(SelectionKey token, ByteBuffer reader) {

		var opcode = (byte)encodeAndDecode.decodeByte(reader);
		String format = OPCodes.getFormatForCode(opcode);
		
		var params = Arrays.stream(format.split(":")).collect(LinkedHashMap::new, (map, e) -> {
		}, Map::putAll);
		if(params.size() == 2) {
			PrivateMessageReader pMReader = new PrivateMessageReader(reader);
			pMReader.process();
			Message received = pMReader.get();
		}
		else if(params.size() == 1) {
			PublicMessageReader pMReader = new PublicMessageReader(reader);
			pMReader.process();
			Message received = pMReader.get();
		}
		else {
			if(opcode == 100) {
				System.out.println("connection acceptée");
			}
			else {
				System.out.println("connection refusée");
			}
		}
	}
	
	private void processOut() {
		while (!queue.isEmpty()) {
			var request = queue.remove();
			request.accept(out);
		}
	}
 
}
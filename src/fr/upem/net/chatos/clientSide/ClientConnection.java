package fr.upem.net.chatos.clientSide;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class ClientConnection {
	private final Selector selector;
	private final SocketChannel clientchannel;
	private final InetSocketAddress remoteServer;
	private final LinkedList<Runnable> pendingActions = new LinkedList<>();
	private boolean closed;
	private ClientContext connectionContext;

	private ClientConnection(InetSocketAddress remoteAddress, SocketChannel channel, Selector selector) {
		this.remoteServer = Objects.requireNonNull(remoteAddress);
		this.clientchannel = Objects.requireNonNull(channel);
		this.selector = Objects.requireNonNull(selector);
	}

	public static ClientConnection startNew(String host, int port) throws IOException {
		return new ClientConnection(new InetSocketAddress(host, port), SocketChannel.open(), Selector.open());
	}

	public void onConnect(Runnable action) {
		if (connectionContext != null) {
			action.run();
		} else {
			pendingActions.add(action);
		}
	}

	public void request(OPCodes opcode, Map<String, Object> params) {
		if (connectionContext != null) {
			connectionContext.write(opcode, params);
			selector.wakeup();			
		} else {
			pendingActions.add(() -> {
				connectionContext.write(opcode, params);
				selector.wakeup();	
			});
		}
	}

	public void connect(Function<SelectionKey, ClientContext> accept) throws IOException {
		System.out.println(String.format("Tentative de connection à : " +remoteServer.getHostName()+":"+ remoteServer.getPort()));

		var thread = new Thread(() -> {
			try {
				clientchannel.configureBlocking(false);
				clientchannel.connect(remoteServer);
				clientchannel.register(selector, SelectionKey.OP_CONNECT);
	
				while (!Thread.interrupted() && !closed) {
					try {
						selector.select(token -> {
							this.actionListener(token, accept);
						});
					} catch (UncheckedIOException uioe) {
						throw uioe.getCause();
					}
				}
				clientchannel.close();	
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}); 
		thread.start();
	}

	public void disconnect() {
		closed = true;
		selector.wakeup();
	}

	private void actionListener(SelectionKey key, Function<SelectionKey, ClientContext> accept) {
		if (key.isValid() && key.isConnectable()) {
			try {	
				if (!((SocketChannel)key.channel()).finishConnect()) {
					System.out.println("La connection avec le serveur à échoué");
					return;
				}
			} catch (IOException ioe) {
				throw new UncheckedIOException(ioe); 
			}
			
			connectionContext = accept.apply(key);
			connectionContext.updateInterestOps();
			key.attach(connectionContext);
			
			pendingActions.forEach(action -> action.run());
			pendingActions.clear();
		}

		try {
			var connection = (ClientContext) key.attachment();
			if (key.isValid() && key.isWritable()) {
				connection.doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				connection.doRead();
			}
		} catch (IOException e) {
			System.out.println("connection coupée due à une IOException" + e);
		}
	}

}

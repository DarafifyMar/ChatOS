package fr.upem.net.chatos.readers;

import java.nio.ByteBuffer;

public class PublicMessageReader implements Reader<Message>{
	private enum State {
		DONE, WAITING_TEXT, ERROR
	};

	private State state = State.WAITING_TEXT;
	private String login;
	private final ByteBuffer bb;
	private final StringReader textReader;
	
	private String text;

	public PublicMessageReader(ByteBuffer bb) {
		this.bb = bb;
		this.textReader = new StringReader(bb);
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		
		bb.flip();
		
		try {
			if ( state == State.WAITING_TEXT ) {
				switch ( textReader.process() ) {
					case DONE:
					   	text = (String) textReader.get();
					   	state = State.DONE;
					    break;
					case ERROR:
						state = State.ERROR;
						return ProcessStatus.ERROR;
					case REFILL:
						return ProcessStatus.REFILL;
				}
			}

			return ProcessStatus.DONE;
		} finally {
			bb.compact();
		}

	}

	@Override
	public Message get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new Message(login, text);
	}

	public void from(String login) {
		this.login = login;
	}
	@Override
	public void reset() {
		textReader.reset();
		state = State.WAITING_TEXT;
	}
}

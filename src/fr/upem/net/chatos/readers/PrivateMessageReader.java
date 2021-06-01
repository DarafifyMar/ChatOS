package fr.upem.net.chatos.readers;

import java.nio.ByteBuffer;

public class PrivateMessageReader implements Reader<Message>{
	private enum State {
		DONE, WAITING_LOGIN, WAITING_TEXT, ERROR
	};

	private State state = State.WAITING_LOGIN;
	
	private final ByteBuffer bb;
	private final StringReader loginReader;
	private final StringReader textReader;
	
	private String login;
	private String text;

	public PrivateMessageReader(ByteBuffer bb) {
		this.bb = bb;
		this.loginReader = new StringReader(bb);
		this.textReader = new StringReader(bb);
	}

	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		
		bb.flip();
		
		try {
			if ( state == State.WAITING_LOGIN ) {
				switch ( loginReader.process() ) {
					case DONE:
					   login = (String) loginReader.get();
					   state = State.WAITING_TEXT;
					   break;
					case ERROR:
						state = State.ERROR;
						return ProcessStatus.ERROR;
					case REFILL:
						return ProcessStatus.REFILL;
				}
			}
			
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

	@Override
	public void reset() {
		loginReader.reset();
		textReader.reset();
		state = State.WAITING_LOGIN;
	}
}

package fr.upem.net.chatos.readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String> {

	private enum State {
		DONE, WAITING_SIZE, WAITING_TEXT, ERROR
	};

	private final ByteBuffer bb;
	private State state = State.WAITING_SIZE;
	private int size;
	private String str;
	private final static Charset UTF_8_CHARSET = StandardCharsets.UTF_8;

	/**
	 * We need in the buffer at least one int for the size of the string encode in
	 * UTF_8 and the following size bytes
	 * 
	 * @param bb the buffer to process and use later.
	 */
	public StringReader(ByteBuffer bb) {
		this.bb = bb;
	}

	/**
	 * Process the bb to get the first int then get and decode the encoded string of
	 * of size found in the first int bb need to be in write mode
	 * 
	 * @return the ProcessStatus
	 * @throws IllegalStateException if the state isn't good when the function is
	 *                               run
	 */
	@Override
	public ProcessStatus process() {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		if (state == State.WAITING_SIZE && bb.remaining() >= Integer.BYTES) {
			size = bb.getInt();

			if (size <= 0) {
				System.out.println("String Error, empty string are not allowed");
				System.out.println("String size : " + size);
				return ProcessStatus.ERROR;
			}
			state = State.WAITING_TEXT;
		}

		if (state == State.WAITING_TEXT && bb.remaining() >= size) {
			var oldLim = bb.limit();
			bb.limit(bb.position() + size);
			str = UTF_8_CHARSET.decode(bb).toString();
			bb.limit(oldLim);
			state = State.DONE;
			return ProcessStatus.DONE;
		} else {
			return ProcessStatus.REFILL;
		}

	}

	/**
	 * Need to be call after the function process returned DONE
	 * 
	 * @return the value decoded in process as an Object need to cast the result
	 * @throws IllegalStateException if the state isn't DONE
	 *
	 */
	@Override
	public String get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return str;
	}

	/**
	 * Reset the state meaning we need new data to process in bb
	 */
	@Override
	public void reset() {
		state = State.WAITING_SIZE;
	}
}
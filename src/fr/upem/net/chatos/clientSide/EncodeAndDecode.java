package fr.upem.net.chatos.clientSide;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class EncodeAndDecode {
	
	private static final Charset UTF8 = Charset.forName("UTF-8");

	final Map<String, BiConsumer<Object, ByteBuffer>> encodersList = Map.of(
		"int", this::encodeInt,
		"byte", this::encodeByte,
		"string", this::encodeString
	);
	final Map<String, Function<ByteBuffer, Object>> decodersList = Map.of(
		"int", this::decodeInt,
		"byte", this::decodeByte,
		"string", this::decodeString
	);
	
	void encodeInt(Object o, ByteBuffer writer) {
		writer.putInt(((Integer)o).intValue());
	}
	
	void encodeByte(Object o, ByteBuffer writer) {
		writer.put(((Byte)o).byteValue());
	}

	void encodeString(Object o, ByteBuffer writer) {
		var s = (String) o;
		writer.putInt(UTF8.encode(s).remaining());
		writer.put(UTF8.encode(s));
	}

	
	Object decodeInt(ByteBuffer reader) {
		reader.flip();
		try {
			return reader.getInt();
		} finally {
			reader.compact();
		}
	}


	public Object decodeByte(ByteBuffer reader) {
		reader.flip();
		try {
			return reader.get();
		} finally {
			reader.compact();
		}
	}

	Object decodeString(ByteBuffer reader) {
		reader.flip();
		try {
			var size = reader.getInt();
			var oldLimit = reader.limit();
			reader.limit(reader.position() + size);
			var s = Charset.forName("UTF-8").decode(reader).toString();
			reader.limit(oldLimit);
			return s;
		} finally {
			reader.compact();
		}
	}
}

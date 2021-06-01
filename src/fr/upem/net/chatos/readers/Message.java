package fr.upem.net.chatos.readers;

import java.util.Objects;

public class Message {
	public final String login;
	public final String text;

	public Message(String login, String text) {
		this.login = Objects.requireNonNull(login);
		this.text = Objects.requireNonNull(text);
	}
}
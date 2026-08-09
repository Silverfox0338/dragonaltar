package com.dragonaltar.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.util.*;

public final class MessageService {
	private final ConfigService config;
	private final MiniMessage mini = MiniMessage.miniMessage();
	public MessageService(ConfigService config) {
		this.config = config;
	}
	public Component component(String key, String... replacements) {
		String raw = config.file("messages.yml").getString(key, "<red>Missing message: " + key + "</red>");
		List<TagResolver> resolvers = new ArrayList<>();
		for (int i = 0; i + 1 < replacements.length; i += 2)
			resolvers.add(Placeholder.unparsed(replacements[i], replacements[i + 1]));
		return mini.deserialize(raw, resolvers.toArray(TagResolver[]::new));
	}
	public void send(CommandSender sender, String key, String... replacements) {
		sender.sendMessage(component(key, replacements));
	}
}

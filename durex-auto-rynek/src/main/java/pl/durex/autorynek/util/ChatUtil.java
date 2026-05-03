package pl.durex.autorynek.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ChatUtil {

    public static void send(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(
                Component.literal(message.replace("&", "§")), false
            );
        }
    }

    public static void sendCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getConnection() != null) {
            if (command.startsWith("/")) {
                client.player.connection.sendCommand(command.substring(1));
            } else {
                client.player.connection.sendChat(command);
            }
        }
    }
}

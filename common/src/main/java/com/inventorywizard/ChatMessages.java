package com.inventorywizard;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;

/** Chat helpers for the update notice. */
final class ChatMessages {

    private ChatMessages() {}

    /** Styles {@code text} as a link that opens {@code url} and shows it on hover. */
    static MutableComponent link(MutableComponent text, URI url) {
        return text.withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                //? if >=1.21.5 {
                .withClickEvent(new ClickEvent.OpenUrl(url))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(url.toString()))));
                //?} else {
                /*.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url.toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(url.toString()))));
                *///?}
    }

    /** Shows {@code message} in the player's chat. The caller checks that there is a player. */
    static void send(Minecraft client, Component message) {
        //? if >=26.1 {
        client.player.sendSystemMessage(message);
        //?} else
        //client.player.displayClientMessage(message, false);
    }
}

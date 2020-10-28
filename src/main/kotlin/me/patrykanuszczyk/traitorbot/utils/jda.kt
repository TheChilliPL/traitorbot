package me.patrykanuszczyk.traitorbot.utils

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.requests.RestAction

val Message.guildOrNull: Guild?
    get() = if (isFromGuild) guild else null

/**
 * The code for the Reaction of this reaction event, shortcut for `reactionEmote.asReactionCode`.
 *
 * For unicode emojis this will be the unicode of said emoji rather than an alias like `:smiley:`.
 *
 * For custom emotes this will be the name and id of said emote in the format `<name>:<id>`.
 *
 * @return The unicode if it is an emoji, or the name and id in the format `<name>:<id>`
 */
val GenericMessageReactionEvent.reactionCode: String
    get() = reactionEmote.asReactionCode

fun Member.moveTo(channel: VoiceChannel): RestAction<Void> = this.guild.moveVoiceMember(this, channel)
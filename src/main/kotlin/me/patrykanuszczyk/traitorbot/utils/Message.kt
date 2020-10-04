package me.patrykanuszczyk.traitorbot.utils

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message

val Message.guildOrNull: Guild?
    get() = if (isFromGuild) guild else null
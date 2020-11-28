package me.patrykanuszczyk.traitorbot.utils

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild

val digits = ('0'..'9').toList()
val bigLetters = ('A'..'Z').toList()
val smallLetters = ('a'..'z').toList()
val alphanumerics = digits + bigLetters + smallLetters

fun getRandomString(characters: Collection<Char>, length: Int): String {
    val sb = StringBuilder(length)
    for (i in 0 until length)
        sb.append(characters.random())
    return sb.toString()
}

enum class FormattingEscapeMode(internal val func: String.() -> String) {
    NORMAL({
        replace(Regex("[_`~*\\\\]|(?<=[\\s^])>"), "\\\\$0")
    }),
    CODE_BLOCK({
        replace("`", "\u200d`\u200d").replace(Regex("\u200d+"), "\u200d")
    })
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.escapeFormattingInCode() = escapeFormatting(FormattingEscapeMode.CODE_BLOCK)
fun String.escapeFormatting(mode: FormattingEscapeMode? = FormattingEscapeMode.NORMAL): String {
    if(mode == null) return this
    return mode.func(this)
}

@Experimental
data class UserMentionEscapeMode(
    val prefix: String,
    val suffix: String,
    val useEffectiveName: Boolean,
    val showDiscriminator: Boolean,
    val unknown: String? = null
) {
    companion object {
        val FULL_TAG = UserMentionEscapeMode(
            "@",
            "",
            useEffectiveName = false,
            showDiscriminator = true
        )
        val USERNAME = UserMentionEscapeMode(
            "",
            "",
            useEffectiveName = true,
            showDiscriminator = false
        )
    }
}

@Experimental
data class RoleMentionEscapeMode(
    val prefix: String,
    val suffix: String,
    val unknown: String? = null
) {
    companion object {
        val WITH_PREFIX = RoleMentionEscapeMode("@", "")
        val NAME = RoleMentionEscapeMode("", "")
    }
}

@Experimental
data class MentionEscapeMode(
    val userMentionMode: UserMentionEscapeMode? = UserMentionEscapeMode.FULL_TAG,
    val roleMentionMode: RoleMentionEscapeMode? = RoleMentionEscapeMode.WITH_PREFIX,
    val escapeMassMentions: Boolean = true,
    val formattingEscapeMode: FormattingEscapeMode? = FormattingEscapeMode.NORMAL
)

val userMentionRegex = Regex("<@!?(\\d+)>")
val roleMentionRegex = Regex("<@&(\\d+)>")
val massMentionRegex = Regex("@(everyone|here)")

@Experimental
fun String.escapeMentions(guild: Guild) = escapeMentions(MentionEscapeMode(), guild)
@Experimental
fun String.escapeMentions(
    mode: MentionEscapeMode,
    guild: Guild
) = escapeMentions(mode, guild.jda, guild)
@Experimental
fun String.escapeMentions(
    mode: MentionEscapeMode = MentionEscapeMode(),
    jda: JDA? = null,
    guild: Guild? = null
): String {
    val (userMode, roleMode, massEsc, escMode) = mode
    var str = this
    if (userMode != null) {
        str = str.replace(userMentionRegex) {
            val userId = it[1]
            val user = try {
                jda!!.retrieveUserById(userId).complete()
            } catch (e: Exception) {
                null
            }
            val name = when {
                user == null -> return@replace userMode.unknown ?: it[0]
                userMode.useEffectiveName -> {
                    guild?.retrieveMember(user)?.complete()?.effectiveName
                        ?: user.name
                }
                else -> user.name
            }
            userMode.prefix + (name + if (userMode.showDiscriminator) "#" + user.discriminator else "")
                .escapeFormatting(escMode) + userMode.suffix
        }
    }
    if (roleMode != null) {
        str = str.replace(roleMentionRegex) {
            val roleId = it[1]
            val role = try {
                if (guild != null) guild.getRoleById(roleId) else jda!!.getRoleById(roleId)
            } catch (e: java.lang.Exception) {
                null
            }
            val name = role?.name ?: return@replace roleMode.unknown ?: it[0]
            roleMode.prefix + name.escapeFormatting(escMode) + roleMode.suffix
        }
    }
    if(massEsc) {
        str = str.replace(massMentionRegex, "@\u200d$1")
    }
    return str
}
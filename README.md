# TraitorBot
A Discord bot written in [Kotlin] (JVM) using [JDA] Discord API wrapper and [HikariCP] database connection pool for [JDBC] (previously, up to 3.1: [Exposed]).  
At the moment, an already hosted bot isn't available to invite, but we plan to make it public once it hits the first stable version 1.0!

  [Kotlin]: https://kotlinlang.org/
     [JDA]: https://github.com/DV8FromTheWorld/JDA
[HikariCP]: https://github.com/brettwooldridge/HikariCP
    [JDBC]: https://en.wikipedia.org/wiki/Java_Database_Connectivity
 [Exposed]: https://github.com/JetBrains/Exposed
 
## Features
Current features of this bot include:
* **Invite detection**  
After enabled, sends a message every time a user joins, saying which invite was used and who's its creator.
* **On-discord MEE6 leaderboard**  
If you have [MEE6] in your server, you no longer have to enter the website MEE6 gives you when you try to check the leaderboard.  
Instead, you can just check it on Discord, as long as its public, using the command `mee6levels` or just `m6l`. 
* **Customizable prefix for each guild**  
To prevent prefix collisions with other bots, by default TraitorBot doesn't have any prefix.  
You can set one for your guild using `@TraitorBot prefix set <prefix>`, as a mention always works like a prefix.
* **Subreddit linker**  
If enabled, TraitorBot will automatically detect messages with subreddits (e.g. [r/discordapp]) and provide clickable links for them.
Notice that it doesn't filter out NSFW subreddits or subreddits that don't exist as it doesn't communicate with Reddit.
* **Voicechat mass move command for administrators**  
Administrators can move many voicechat members from one channel to another using the command:  
`vcmove ChannelFrom ChannelTo`  
where `ChannelFrom` is the name or ID of the channel you want to move people from,
and `ChannelTo`is the name or ID of the channel you want to move people to.  
You can use a single word from one of the channels multiple words if you put them in quotes (e.g. `"Cool music"`) or just copy its ID by right clicking a channel (if you have developer mode turned on in Discord settings).
* **Voicechat roles**  
By using `vcroles` (or `vcr`) command you can set up a role to add everyone who enters the specified voice channel, and to remove from everyone who leaves this channel. It was originally planned to use for a channel used in Among Us for killed players, but can be also used, for example, to provide a no microphone channel, and hide it when someone's not in the voice channel.
* **Voting**  
You can create a reaction voting by using the command `voting new (-n <ID>) <Title> <Answer 1> <Answer 2> <...>` and
close it with the command `voting end <ID>`. Currently, votings can be added and removed by everyone, and they do not
stay open after the bot restarts.

[MEE6]: https://mee6.gg/
[r/discordapp]: https://reddit.com/r/discordapp/

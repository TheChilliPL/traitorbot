# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.5.2] - 2020-11-29

### Fixed

- [Hotfix] SQL query throwing errors when trying to change invite detect module messages.

## [0.5.1] - 2020-11-28

### Security

- [Hotfix] Now the manage server permission is needed to manage the invite detect module.


## [0.5] - 2020-11-28

### Added

- Invite detect module—detects which invites were used when someone joins.
  (fixes #4)
    - Guild members gateway intent added—will require verification from 100 guilds upwards.
    - `invitedetect` (alias `invd`) command for managing it.
        - `invd enable <channel>`: enables invite detection, sends messages about it in the specified channel.
        - `invd disable`: disables invite detection.
        - `invd message/msg <known/unknown> <text>`: changes the join message if the invite used is known or unknown.
            - Available placeholders:
                - `{USER_MENTION}` mentions the user who joined.
                - `{USER_NAME}` is the effective name of the invited user.
                - `{INVITE_CODE}` is the invite code used (`known` only).
                - `{INVITER_MENTION}` mentions the user who created the invite.
                - `{INVITER_NAME}` is the effective name of the inviter.
- Some utility functions, including formatting and mentions escaping.

### Changed

- Improved parameter parsing.
- Inlined some of the utility functions to make them faster.

## [0.4] - 2020-11-25

### Added

- Full ping measurement option (`%ping -f`)
- Subreddit linker module. Turn it on or off using
  `%srlink on/off/enable/disable` and type `r/aww` to check it out!

### Removed

- No longer requires Kotlin Exposed. Instead, using JDBC with HikariCp and cache
  (fixed #7).  
  This made the bot about 3 times faster.

## [0.3.1] - 2020-11-15

### Fixed

- Mee6 levels module not working on some machines due to the default user agent.

## [0.3.0] - 2020-11-15

### Added

- Mee6 module allowing to show the Mee6 leaderboard in an embed.

## [0.2.1] - 2020-10-24

### Added

- `vcmove` command that moves everyone from a specified voice channel to another voice channel.
- `ping` command that calculates API latency.

## [0.2.0] - 2020-10-24

### Changed

- Rebuilt command system.
- Improved voting module.
- Updated logging.

## [0.1.0] - 2020-10-11

Started bot development.

### Added

- Commands system.
- Admin module for managing the bot.
- Prefix module that allows each guild to choose their own prefix.
- Voice chat roles module that gives a role to everyone that joins a specific voice channel and removes it when they
  leave.
- Voting module that allows to make surveys.
![Logo Transparent](/assets/banner_transparent.png)

[![Modrinth Page](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_vector.svg)](https://modrinth.com/project/packperregion)
[![Github Page](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/github_vector.svg)](https://github.com/TheAnnoying/packperregion)
[![Discord Server](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/social/discord-plural_vector.svg)](https://discord.com/)

> [!IMPORTANT]
> You must open **port 8080** on your server.
> The plugin will not function correctly if this port is blocked.

Allows players to define regions with their own custom resource packs.
When a player enters a region, the associated resource pack is automatically applied.

This is ideal for SMP servers where players want customized bases, themed builds, or unique item models without impacting the rest of the world.

## Features

* Region-based resource packs
* Automatic pack application on region entry (optional/required)
* Admin approval for uploaded packs
* Built-in web interface for pack uploads
* Configurable limits per player and maximum pack upload size
* Fully customizable and formattable messages
* Lightweight and uses no external services

## Commands

### Player Commands
```
/registerarea <first corner coordinates> <second corner coordinates>
```

### Admin Commands
```
/packperregion accept <id>
/packperregion delete <id>
/packperregion list
```
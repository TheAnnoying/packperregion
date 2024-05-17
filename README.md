## Description
Pack Per Region is a plugin that allows server players to register regions in which a resource pack will be applied.

## Usage
* **Server Members**
  * Run the command `/registerarea <first corner coordinates> <second corner coordinates>`
  * Open the link sent in chat and upload your resource pack on the website
* **Server Administrators**
  * Use the command `/packperregion packlist` to view all the packs and their tokens
  * The `/packperregion packlist` also includes the resource pack url which you may use to verify the resource pack is appropriate.
  * Use either `/packperregion reject-or-delete <token>` or `/packperregion accept <token>` to reject or accept a resource pack.

## FAQ
* _**Is there a per-player region limit?**_:
  * Players can only have `5` regions. (server administrators can change the limit in the config)
* _**Can you edit your regions?**_:
  * Not really. After you uploaded your resource pack, you can only delete your region. If you'd like to change your coordinates or resource pack, delete your region and make a new one.
* _**Is there a pack size limit?**_:
  * Yes, 100MB.
* _**What happens to rejected packs?**_:
  * Packs that get rejected are deleted from our storage.
* _**How do I know my pack has been accepted?**_:
  * The plugin will not tell you when your resource pack has been accepted.

## Credits
Check out [Lianecx](https://github.com/Lianecx) and his plugin [Discord-Linker](https://modrinth.com/plugin/Discord-Linker/)! He helped a lot with this project.
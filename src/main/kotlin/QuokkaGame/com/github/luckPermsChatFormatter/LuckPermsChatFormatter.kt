package QuokkaGame.com.github.luckPermsChatFormatter

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.Listener

class LuckPermsChatFormatter : JavaPlugin(), Listener {

    private lateinit var luckPerms: LuckPerms
    private lateinit var chatFormat: String

    override fun onEnable() {
        // Plugin startup logic
        CommandAPI.onEnable()
        logger.info("LuckPermsChatFormatter enabled!")

        reloadCommand()
        saveDefaultConfig()
        server.pluginManager.registerEvents(this, this)

        chatFormat = config.getString("format", "<{prefix}{name}{suffix}> message").toString()

        try {
            luckPerms = LuckPermsProvider.get()
        } catch (e: Exception) {
            logger.severe("§cFailed to get LuckPerms API. Please make sure LuckPerms is installed correctly.")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
            return
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
        CommandAPI.onDisable()
        logger.info("LuckPermsChatFormatter disabled!")
    }

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        val sender = event.player

        val user = luckPerms.userManager.getUser(sender.uniqueId)
        val prefix = user?.cachedData?.metaData?.prefix ?: ""
        val suffix = user?.cachedData?.metaData?.suffix ?: ""

        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        val formattedMessage = formatChat(sender, prefix, suffix, message)

        event.renderer { _, _, _, _ -> LegacyComponentSerializer.legacySection().deserialize(formattedMessage) }
    }

    private fun formatChat(player: Player, prefix: String, suffix: String, message: String): String {
        val displayName = LegacyComponentSerializer.legacySection().serialize(player.displayName())
        return chatFormat
            .replace("{prefix}", prefix)
            .replace("{suffix}", suffix)
            .replace("{name}", displayName)
            .replace("{displayname}", displayName)
            .replace("{message}", message)
    }

    private fun reloadCommand() {
        CommandAPICommand("luckpermschatformatter")
            .withPermission("luckpermschatformatter.reload")
            .withAliases("lpcf")
            .withUsage("§3/luckpermschatformatter reload")
            .withShortDescription("LuckPermsChatFormatter reload command")
            .executesPlayer(PlayerCommandExecutor { player, _ ->
                player.sendMessage("§3/luckpermschatformatter reload")
            })
            .withSubcommand(
                CommandAPICommand("reload")
                    .executesPlayer(PlayerCommandExecutor { player, _ ->
                        reloadConfig()
                        chatFormat = config.getString("format", "<{prefix}{name}{suffix}> message").toString()
                        player.sendMessage("§aLuckPermsChatFormatter config reloaded!")
                    })
            )
            .register()
    }
}


import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.typesafe.config.ConfigRenderOptions
import config.DiscordConfig
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.kordLogger
import extensions.ExtensionsExtension
import extensions.PrivateRoomsExtension
import extensions.ReloadExtension
import extensions.RoleTakingExtension
import io.github.config4k.toConfig
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.system.exitProcess

suspend fun main() {
	val configPath = Config.path.toPath()

	if (!FileSystem.SYSTEM.exists(configPath)) {
		kordLogger.warn("Config not found, creating...")

		val renderOptions = ConfigRenderOptions.defaults()
			.setJson(false)
			.setOriginComments(false)

		FileSystem.SYSTEM.write(configPath, true) {
			writeUtf8(
				DiscordConfig().toConfig("discord")
					.root().render(renderOptions)
			)
		}

		kordLogger.warn("Configure the config!")
		exitProcess(1)
	}

	Config.update()
	val discordConfig = Config.current

	val bot = ExtensibleBot(discordConfig.token) {
		applicationCommands {
			defaultGuild(discordConfig.guildId.toULong())
		}

		extensions {
			add(::RoleTakingExtension)
			add(::ExtensionsExtension)
			add(::PrivateRoomsExtension)
			add(::ReloadExtension)
		}
	}

	Config.loadDisabledExtensions()
	Config.disabledExtensions.forEach { extensionName ->
		bot.unloadExtension(extensionName)
	}

	bot.on<ReadyEvent> {
		kordLogger.info("Bot ready")
	}

	bot.start()
}
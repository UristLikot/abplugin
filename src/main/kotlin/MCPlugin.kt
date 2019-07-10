import com.google.common.reflect.TypeToken
import com.google.inject.Inject
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.gson.GsonConfigurationLoader
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.Sponge.getCommandManager
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.command.args.GenericArguments
import org.spongepowered.api.command.spec.CommandExecutor
import org.spongepowered.api.command.spec.CommandSpec
import org.spongepowered.api.config.ConfigDir
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GameInitializationEvent
import org.spongepowered.api.event.game.state.GameStartedServerEvent
import org.spongepowered.api.event.game.state.GameStartingServerEvent
import org.spongepowered.api.event.network.ClientConnectionEvent
import org.spongepowered.api.event.user.BanUserEvent
import org.spongepowered.api.plugin.Dependency
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.profile.ProfileNotFoundException
import org.spongepowered.api.service.ban.BanService
import org.spongepowered.api.text.Text
import org.spongepowered.api.util.ban.Ban
import org.spongepowered.api.util.ban.BanTypes
import java.io.File
import java.nio.file.Path
import java.sql.Array
import java.time.Duration
import java.time.Instant
import java.util.*
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.reflect.typeOf

val db = DbHandler()

@Plugin(
    id = "abplugin",
    name = "abplugin",
    version = "0.0.1",
    description = "My first plugin",
    authors = ["UristLikot"],
    dependencies = arrayOf(
        Dependency(
            id = "spotlin",
            optional = false,
            version = "0.2.0"
        )
    )
)
class mcplugin {
    @Inject
    lateinit var logger: Logger

    @Listener
    fun onServerStart(event: GameInitializationEvent) {
        try {
            db.createDb()
        } catch (e: Exception) {
            println("Can't create DB")
        }

        logger.info("Successfully running abplugin.")


    }


    @Listener
    fun loadToDb(event: BanUserEvent) {
        try {
            event.ban.profile
            val uuid = event.ban.profile.uniqueId.toString()
            val name = event.ban.profile.name.get()
            val created = event.ban.creationDate
            val myDateCr = Date.from(created)
            var source = event.ban.banSource.get().toString()
            val expires = event.ban.expirationDate.get()
            val myDateExp = Date.from(expires)
            val reason = event.ban.reason.get().toString().replace("Text{", "").replace("}", "")
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val finCr = formatter.format(myDateCr)
            val finExp = formatter.format(myDateExp)
            if ("Unknown" in source) {
                source = "Server"
            }
            db.addBanToDb(uuid, name, finCr, source, finExp, reason)
        } catch (e: Exception) {
            println("Can't add user to DB.")
        }
    }

    @Listener
    fun checkBans(event: ClientConnectionEvent.Join) {
        if (Sponge.getServer().getPlayer(event.targetEntity.name).isPresent) {
            val playerProfile = Sponge.getServer().getPlayer(event.targetEntity.name).get().profile
            val playerName = Sponge.getServer().getPlayer(event.targetEntity.name).get().name
            if (service.isBanned(playerProfile)) {
                val optionalBan = service.getBanFor(playerProfile)
                if (optionalBan.isPresent) {
                    val profileBan = optionalBan.get()
                    val optionalReason = profileBan.reason
                    if (optionalReason.isPresent) {
                        val banReason = optionalReason.get()
                        Sponge.getServer().getPlayer(playerName).get().kick(banReason)
                    }
                }
            }
        }

    }

    @Listener
    fun regBan(event: GameInitializationEvent) {
        try {

            getCommandManager().register(this, banCommand, "aban")
            logger.info("Successfully registered aban.")
        } catch (e: Exception) {
            println("Can't register aban")
        }

    }

    @Listener
    fun refrBan(event: GameInitializationEvent) {

        try {
            getCommandManager().register(this, refBanCommand, "refbans")
            logger.info("Successfully registered refresh.")
        } catch (e: Exception) {
            println("Can't register uban")
        }

    }

    @Listener
    fun reguBan(event: GameInitializationEvent) {

        try {
            getCommandManager().register(this, ubanCommand, "uban")
            logger.info("Successfully registered refban.")
        } catch (e: Exception) {
            println("Can't register refban")
        }

    }

}

class BanCommand : CommandExecutor {
    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        try {
            val pName = args.getOne<String>("player").get()
            val pn = Sponge.getServer().gameProfileManager.get(pName).get()
            val message = args.getAll<String>("reason").toString().replace("[", "").replace("]", "")
            val time = args.getAll<Double>("time(hours)").toString().replace("[", "").replace("]", "").toDouble()
            val instant = Instant.ofEpochMilli(Date().getTime()).plus(Duration.ofHours(time.toLong()))
            service.addBan(
                (Ban.builder()
                    .type(BanTypes.PROFILE)
                    .profile(pn)
                    .source(Text.of(src.name))
                    .reason(Text.of(message))
                    .expirationDate(instant)
                    .build())
            )
            if (Sponge.getServer().getPlayer(pName).isPresent) {
                Sponge.getServer().getPlayer(pName).get().kick()
            }
        } catch (e: ProfileNotFoundException) {
            println("Player not found")
        }
        return CommandResult.success()
    }


}

class uBanCommand : CommandExecutor {

    override fun execute(src: CommandSource, args: CommandContext): CommandResult {
        try {
            val pName = args.getOne<String>("player").get()
            val pn = Sponge.getServer().gameProfileManager.get(pName).get()
            service.pardon(pn)
            db.removeFromDb(pName)
        } catch (e: Exception) {
            println("Player not found")
        }
        return CommandResult.success()
    }
}

class refreshBanCommand : CommandExecutor {

    override fun execute(src: CommandSource, args: CommandContext): CommandResult {

        val configPath = File("banned-players.json")
        val potentialFile = GsonConfigurationLoader.builder().setFile(configPath).build()
        val gsonList = potentialFile.load()
        val bList = mutableListOf<String>()
        val mp = mutableMapOf<String, String>()
        val banList = gsonList.getNode().getList { shit ->
            bList.add(shit.toString())
        }

        for (i in bList) {
            val j = i.splitToSequence(",", ignoreCase = true, limit = 6)
            for (k in j) {
                val f = k.replace("{", "").replace("}", "").splitToSequence("=", limit = 2).toSet().toTypedArray()
                mp.put(f[0].trim(), f[1])

            }
            db.refrBansDb(mp)
        }
        return CommandResult.success()
    }
}


val service = Sponge.getServiceManager().provide(BanService::class.java).get()
val banCommand = CommandSpec.builder()
    .arguments(
        GenericArguments.onlyOne(GenericArguments.string(Text.of("player"))),
        GenericArguments.doubleNum(Text.of("time(minutes)")),
        GenericArguments.flags().permissionFlag("abplugin.aban", "s").buildWith(GenericArguments.none()),
        GenericArguments.remainingJoinedStrings(Text.of("reason"))
    )
    .executor(BanCommand())
    .build()

val ubanCommand = CommandSpec.builder()
    .arguments(
        GenericArguments.flags().permissionFlag("abplugin.uban", "s")
            .buildWith(GenericArguments.onlyOne(GenericArguments.string(Text.of("player"))))
    )
    .executor(uBanCommand())
    .build()
val refBanCommand = CommandSpec.builder()
    .arguments(
        GenericArguments.flags().permissionFlag(
            "abplugin.refbans",
            "s"
        ).buildWith(GenericArguments.none())
    )
    .executor(refreshBanCommand())
    .build()

package dev.etran.bedwarswinfix

import com.andrei1058.bedwars.api.BedWars
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class WinFixPlugin : JavaPlugin() {

    private lateinit var settings: Settings
    private var listener: EndGameListener? = null

    override fun onEnable() {
        val registration = Bukkit.getServicesManager().getRegistration(BedWars::class.java)
        if (registration == null) {
            logger.severe("BedWars1058 API is not available. Disabling.")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Config lives in plugins/BedWars1058/Addons/WinFix/config.yml, not in plugins/BedWars1058-WinFix/.
        settings = loadSettings(registration.provider.addonsPath)

        val tracker = KillTracker()
        val renderer = SidebarRenderer(registration.provider, TopStatsSource(tracker)) { settings }
        val endGameListener = EndGameListener(this, tracker, renderer) { settings }
        server.pluginManager.registerEvents(endGameListener, this)
        listener = endGameListener

        logger.info("Enabled. End-game sidebars will show the real winner and top list.")
    }

    override fun onDisable() {
        listener?.shutdown()
        listener = null
    }

    private fun loadSettings(addonsPath: File): Settings {
        val dir = File(addonsPath, "WinFix")
        if (!dir.exists() && !dir.mkdirs()) {
            logger.warning("Could not create ${dir.path}; using default settings.")
            return Settings()
        }
        val file = File(dir, "config.yml")
        if (!file.exists()) {
            getResource("config.yml")?.use { input -> file.outputStream().use { input.copyTo(it) } }
        }
        return Settings.from(YamlConfiguration.loadConfiguration(file))
    }
}

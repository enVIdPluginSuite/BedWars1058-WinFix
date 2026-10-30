package dev.etran.bedwarswinfix

import org.bukkit.configuration.file.FileConfiguration

/** Typed view over config.yml. */
data class Settings(
    val orderBy: String = "",
    val hideZeroValues: Boolean = false,
    val expandRows: Boolean = true,
    val maxSidebarLines: Int = 15,
    val refreshDelayTicks: Long = 10L,
    val debug: Boolean = false,
) {
    companion object {
        fun from(config: FileConfiguration): Settings = Settings(
            orderBy = config.getString("order-by", "")?.trim().orEmpty(),
            hideZeroValues = config.getBoolean("hide-zero-values", false),
            expandRows = config.getBoolean("expand-rows", true),
            maxSidebarLines = config.getInt("max-sidebar-lines", 15).coerceIn(1, 40),
            refreshDelayTicks = config.getLong("refresh-delay-ticks", 10L).coerceAtLeast(1L),
            debug = config.getBoolean("debug", false),
        )
    }
}

package dev.etran.bedwarswinfix

import com.andrei1058.bedwars.api.arena.GameState
import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.events.gameplay.GameEndEvent
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent
import com.andrei1058.bedwars.api.events.player.PlayerJoinArenaEvent
import com.andrei1058.bedwars.api.events.player.PlayerKillEvent
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask

class EndGameListener(
    private val plugin: JavaPlugin,
    private val tracker: KillTracker,
    private val renderer: SidebarRenderer,
    private val settings: () -> Settings,
) : Listener {

    /** One pending rebuild per arena; scheduling again replaces the previous one. */
    private val pending = HashMap<String, BukkitTask>()

    @EventHandler(priority = EventPriority.MONITOR)
    fun onKill(event: PlayerKillEvent) {
        val killer = event.killer ?: return
        tracker.addKill(event.arena, killer)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onStateChange(event: GameStateChangeEvent) {
        val arena = event.arena
        when (event.newState) {
            // New match starting: forget the previous match's kills.
            GameState.waiting, GameState.starting -> cancelAndForget(arena)
            GameState.playing -> if (event.oldState == GameState.starting) tracker.reset(arena)
            GameState.restarting -> scheduleRefresh(arena)
            else -> Unit
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onGameEnd(event: GameEndEvent) {
        scheduleRefresh(event.arena)
    }

    // BedWars1058 rebuilds a player's sidebar on these events, which would bring the raw
    // placeholders back, so the end-game sidebar is rebuilt again afterwards.
    @EventHandler(priority = EventPriority.MONITOR)
    fun onJoin(event: PlayerJoinArenaEvent) {
        if (event.arena.status == GameState.restarting) scheduleRefresh(event.arena)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onLeave(event: PlayerLeaveArenaEvent) {
        if (event.arena.status == GameState.restarting) scheduleRefresh(event.arena)
    }

    fun shutdown() {
        pending.values.forEach { it.cancel() }
        pending.clear()
    }

    private fun cancelAndForget(arena: IArena) {
        pending.remove(arena.arenaName)?.cancel()
        tracker.reset(arena)
    }

    private fun scheduleRefresh(arena: IArena) {
        val key = arena.arenaName
        pending.remove(key)?.cancel()
        pending[key] = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            pending.remove(key)
            try {
                val rebuilt = renderer.render(arena)
                if (settings().debug) {
                    plugin.logger.info("Rebuilt $rebuilt end-game sidebar(s) for arena '$key'.")
                }
            } catch (t: Throwable) {
                plugin.logger.warning("Could not rebuild the end-game sidebar for '$key': $t")
                if (settings().debug) t.printStackTrace()
            }
        }, settings().refreshDelayTicks)
    }
}

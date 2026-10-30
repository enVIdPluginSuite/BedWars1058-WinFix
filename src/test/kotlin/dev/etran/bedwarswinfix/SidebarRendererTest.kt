package dev.etran.bedwarswinfix

import com.andrei1058.bedwars.api.BedWars
import com.andrei1058.bedwars.api.arena.IArena
import com.andrei1058.bedwars.api.arena.team.ITeam
import com.andrei1058.bedwars.api.arena.team.TeamColor
import com.andrei1058.bedwars.api.language.Language
import java.lang.reflect.Proxy
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SidebarRendererTest {

    private fun team(name: String, color: TeamColor): ITeam = Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(ITeam::class.java),
    ) { _, method, _ ->
        when (method.name) {
            "getDisplayName" -> name
            "getColor" -> color
            else -> null
        }
    } as ITeam

    private fun arena(winner: ITeam?): IArena = Proxy.newProxyInstance(
        javaClass.classLoader, arrayOf(IArena::class.java),
    ) { _, method, _ -> if (method.name == "getWinner") winner else null } as IArena

    private class Fake(override val username: String, private val kills: Int, override val team: ITeam?) : TopEntry {
        override val uuid: UUID = UUID.randomUUID()
        override val displayName: String = "[$username]"
        override val numeric: Long = kills.toLong()
        override fun value(statId: String, language: Language?): String = if (statId == "kills") "$kills" else "x$kills"
    }

    private fun renderer(): SidebarRenderer {
        val api = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(BedWars::class.java)) { _, _, _ -> null } as BedWars
        return SidebarRenderer(api, TopStatsSource(KillTracker())) { Settings() }
    }

    /** The exact end-game template from the screenshot: 15 lines, 8 "top" rows. */
    private val template: List<String> = listOf(
        "30/10/26", "", "Winner: {winnerTeamColor}{winnerTeamName} *", "", "Top Kills:",
    ) + List(8) { "{topTeamColor}{topPlayerDisplayName} - {topValue}" } + listOf("", "bedwars.mc.etran.dev")

    private val red = team("Red", TeamColor.RED)
    private val blue = team("Blue", TeamColor.BLUE)

    @Test
    fun `fewer players than rows drops unused rows and fills winner`() {
        val entries = listOf(Fake("A", 5, red), Fake("B", 2, blue), Fake("C", 0, blue))
        val out = renderer().renderLines(arena(red), null, template, entries, "kills", Settings())

        assertEquals(15 - 5, out.size)
        assertEquals("Winner: §cRed *", out[2])
        assertEquals("§c[A] - 5", out[5])
        assertEquals("§9[B] - 2", out[6])
        assertEquals("§9[C] - 0", out[7])
        assertEquals("", out[8])
        assertEquals("bedwars.mc.etran.dev", out.last())
        assertFalse(out.any { it.contains("{") })
    }

    @Test
    fun `exactly as many players as rows keeps every row`() {
        val entries = (1..8).map { Fake("P$it", 9 - it, red) }
        val out = renderer().renderLines(arena(red), null, template, entries, "kills", Settings())
        assertEquals(template.size, out.size)
        assertEquals("§c[P8] - 1", out[12])
    }

    @Test
    fun `more players than rows expands while the sidebar has room`() {
        val shortTemplate = template.filterIndexed { i, _ -> i != 1 && i != 3 } // 13 lines
        val entries = (1..12).map { Fake("P$it", 20 - it, red) }
        val out = renderer().renderLines(arena(red), null, shortTemplate, entries, "kills", Settings())
        assertEquals(15, out.size) // capped at max-sidebar-lines
        assertEquals("bedwars.mc.etran.dev", out.last())
        assertEquals("§c[P10] - 10", out[13 - 1 + 1 - 1 + 1 - 1 + 0].let { out[12] }) // last top row sits right before the footer
    }

    @Test
    fun `no expansion when disabled or when the sidebar is already full`() {
        val entries = (1..12).map { Fake("P$it", 20 - it, red) }
        val full = renderer().renderLines(arena(red), null, template, entries, "kills", Settings())
        assertEquals(template.size, full.size)
        val noExpand = renderer().renderLines(arena(red), null, template.drop(1), entries, "kills", Settings(expandRows = false))
        assertEquals(template.size - 1, noExpand.size)
    }

    @Test
    fun `other statistic placeholders are resolved per player`() {
        val out = renderer().renderLines(
            arena(red), null, listOf("{topPlayerName} {topValue-finalKills}"), listOf(Fake("A", 3, red)), "kills", Settings(),
        )
        assertEquals(listOf("A x3"), out)
    }
}

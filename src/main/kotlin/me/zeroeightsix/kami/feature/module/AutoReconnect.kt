package me.zeroeightsix.kami.feature.module

import io.github.fablabsmc.fablabs.api.fiber.v1.annotation.Setting
import me.zero.alpine.listener.EventHandler
import me.zero.alpine.listener.EventHook
import me.zero.alpine.listener.Listener
import me.zeroeightsix.kami.event.events.ScreenEvent
import me.zeroeightsix.kami.event.events.ScreenEvent.Displayed
import me.zeroeightsix.kami.mixin.client.IDisconnectedScreen
import net.minecraft.client.gui.screen.ConnectScreen
import net.minecraft.client.gui.screen.DisconnectedScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.options.ServerEntry
import kotlin.math.floor

/**
 * Created by 086 on 9/04/2018.
 */
@Module.Info(
    name = "AutoReconnect",
    description = "Automatically reconnects after being disconnected",
    category = Module.Category.MISC,
    alwaysListening = true
)
object AutoReconnect : Module() {

    private var cServer: ServerEntry? = null
    @Setting
    private var seconds: @Setting.Constrain.Range(min = 0.0) Int = 5;

    @EventHandler
    val closedListener =
        Listener(
            EventHook { event: ScreenEvent.Closed ->
                if (event.screen is ConnectScreen) cServer =
                    mc.currentServerEntry
            }
        )

    @EventHandler
    val displayedListener = Listener(
        EventHook { event: Displayed ->
            if (isEnabled() && event.screen is DisconnectedScreen && event.screen !is KamiDisconnectedScreen && (cServer != null || mc.currentServerEntry != null)) event.screen =
                KamiDisconnectedScreen(event.screen as DisconnectedScreen)
        }
    )

    private class KamiDisconnectedScreen(disconnected: DisconnectedScreen) : DisconnectedScreen(
        (disconnected as IDisconnectedScreen).parent,
        disconnected.title.asString(),
        (disconnected as IDisconnectedScreen).reason
    ) {
        private val parent: Screen
        var millis = seconds.toLong() * 1000
        var cTime: Long

        override fun tick() {
            if (millis <= 0) mc.openScreen(
                ConnectScreen(
                    parent,
                    mc,
                    if (cServer == null) mc.currentServerEntry else cServer
                )
            )
        }

        override fun render(mouseX: Int, mouseY: Int, partialTicks: Float) {
            super.render(mouseX, mouseY, partialTicks)
            val a = System.currentTimeMillis()
            millis -= (a - cTime).toInt()
            cTime = a
            val s = "Reconnecting in " + 0.0.coerceAtLeast(floor(millis.toDouble() / 100) / 10) + "s"
            font.drawWithShadow(s, width / 2 - font.getStringWidth(s) / 2.toFloat(), height - 16.toFloat(), 0xffffff)
        }

        init {
            cTime = System.currentTimeMillis()
            parent = (disconnected as IDisconnectedScreen).parent
        }

    }
}

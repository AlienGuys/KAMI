package me.zeroeightsix.kami.gui.windows

import glm_.func.common.clamp
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import imgui.ColorEditFlag
import imgui.ImGui
import imgui.ImGui.button
import imgui.ImGui.colorConvertHSVtoRGB
import imgui.ImGui.colorConvertRGBtoHSV
import imgui.ImGui.colorEdit3
import imgui.ImGui.dragFloat
import imgui.ImGui.dummy
import imgui.ImGui.popID
import imgui.ImGui.pushID
import imgui.ImGui.sameLine
import imgui.ImGui.textWrapped
import imgui.TabBarFlag
import imgui.WindowFlag
import imgui.api.demoDebugInformations
import imgui.dsl.checkbox
import imgui.dsl.tabBar
import imgui.dsl.tabItem
import imgui.dsl.window
import io.github.fablabsmc.fablabs.api.fiber.v1.annotation.Setting
import me.zeroeightsix.kami.feature.FindSettings
import me.zeroeightsix.kami.feature.hidden.PrepHandler
import me.zeroeightsix.kami.gui.Themes
import me.zeroeightsix.kami.gui.charButton
import me.zeroeightsix.kami.gui.widgets.EnabledWidgets
import me.zeroeightsix.kami.gui.widgets.TextPinnableWidget
import me.zeroeightsix.kami.gui.windows.modules.ModuleWindowsEditor
import me.zeroeightsix.kami.gui.windows.modules.Modules
import me.zeroeightsix.kami.setting.KamiConfig
import me.zeroeightsix.kami.setting.settingInterface
import kotlin.reflect.KMutableProperty0

@FindSettings
object Settings {

    @Setting
    var settingsWindowOpen = false

    @Setting
    var commandPrefix = '.'

    @Setting
    var openChatWhenCommandPrefixPressed = true

    // Behaviour
    @Setting
    var modifiersEnabled = false

    @Setting
    var openSettingsInPopup = true

    @Setting // only if openSettingsInPopup = false
    var swapModuleListButtons = false

    @Setting
    var hideModuleDescriptions = false

    // Appearance
    @Setting
    var font: Int = 0

    @Setting
    var rainbowMode = false

    @Setting
    var styleIdx = 0

    @Setting
    var borderOffset = 10f

    @Setting
    var rainbowSpeed = 0.1

    @Setting
    var rainbowSaturation = 0.5f

    @Setting
    var rainbowBrightness = 1f

    // Other
    @Setting
    var demoWindowVisible = false

    @Setting
    var hudWithDebug = false

    @Setting
    var moduleAlignment = TextPinnableWidget.Alignment.CENTER

    val themes = Themes.Variants.values().map { it.name.toLowerCase().capitalize() }

    operator fun invoke() {
        fun boolSetting(
            label: String,
            checked: KMutableProperty0<Boolean>,
            description: String,
            block: () -> Unit = {}
        ) {
            checkbox(label, checked, block)
            sameLine()
            demoDebugInformations.helpMarker(description)
        }

        if (settingsWindowOpen) {
            window("Settings", ::settingsWindowOpen, flags = WindowFlag.AlwaysAutoResize.i) {
                tabBar("kami-settings-tabbar", TabBarFlag.None.i) {
                    tabItem("Behaviour") {
                        charButton("Command prefix", ::commandPrefix)
                        sameLine()
                        demoDebugInformations.helpMarker("The character used to denote KAMI commands.")

                        boolSetting(
                            "Open chat when command prefix pressed",
                            ::openChatWhenCommandPrefixPressed,
                            "Opens the chat with the command prefix already inserted when the command prefix is pressed ingame."
                        )

                        boolSetting(
                            "Keybind modifiers",
                            ::modifiersEnabled,
                            "Allows the use of keybinds with modifiers: e.g. chaining CTRL, ALT and K."
                        )
                        boolSetting(
                            "Settings popup",
                            ::openSettingsInPopup,
                            "Show module settings in a popup instead of a collapsible"
                        )
                        // Swap list buttons only applies to the tree header list
                        if (!openSettingsInPopup) {
                            boolSetting(
                                "Swap list buttons",
                                ::swapModuleListButtons,
                                "When enabled, right clicking modules will reveal their settings menu. Left clicking will toggle the module."
                            )
                        }
                        boolSetting(
                            "Hide descriptions",
                            ::hideModuleDescriptions,
                            "Hide module descriptions when its settings are opened."
                        )
                        dummy(Vec2(0, 5))
                        if (button("Reset module windows")) {
                            Modules.reset()
                        }
                        if (!ModuleWindowsEditor.open) {
                            sameLine()
                            if (button("Open module windows editor")) {
                                ModuleWindowsEditor.open = true
                            }
                        }
                    }
                    tabItem("Appearance") {
                        showFontSelector("Font###kami-settings-font-selector")

                        if (ImGui.combo("Theme", ::styleIdx, themes)) {
                            Themes.Variants.values()[styleIdx].applyStyle(true)
                        }
                        
                        KamiConfig.alignmentType.settingInterface?.displayImGui("Module alignment", this.moduleAlignment)?.let {
                            this.moduleAlignment = it
                        }

                        boolSetting(
                            "Rainbow mode",
                            ::rainbowMode,
                            "If enabled, turns the GUI into a rainbow-coloured mess"
                        ) {
                            Themes.Variants.values()[styleIdx].applyStyle(false)
                        }

                        dragFloat(
                            "Border offset",
                            ::borderOffset,
                            vSpeed = 0.1f,
                            vMin = 0f,
                            vMax = 50f,
                            format = "%.0f"
                        )

                        val speed = floatArrayOf(rainbowSpeed.toFloat())
                        if (dragFloat("Rainbow speed", speed, 0, vSpeed = 0.005f, vMin = 0.05f, vMax = 1f)) {
                            rainbowSpeed = speed[0].toDouble().clamp(0.0, 1.0)
                        }

                        val col = Vec4(PrepHandler.getRainbowHue(), rainbowSaturation, rainbowBrightness, 1.0f)
                        colorConvertHSVtoRGB(col)
                        colorEdit3(
                            "Rainbow colour",
                            col,
                            ColorEditFlag.DisplayHSV or ColorEditFlag.NoPicker
                        )
                        val array = col.array
                        colorConvertRGBtoHSV(array, array)
                        rainbowSaturation = array[1]
                        rainbowBrightness = array[2]

                        dummy(Vec2(0, 5))
                        textWrapped("Enabled HUD elements:")
                        EnabledWidgets.enabledButtons()
                    }
                    tabItem("Other") {
                        boolSetting(
                            "Show demo window in 'View'",
                            ::demoWindowVisible,
                            "Allows the demo window to be shown through the 'View' submenu of the main menu bar"
                        )
                        boolSetting(
                            "Show HUD with debug screen",
                            ::hudWithDebug,
                            "Shows the HUD even when the debug screen is open"
                        )
                    }
                }
            }
        }
    }

    fun showFontSelector(label: String) {
        val fontCurrent = ImGui.font
        if (ImGui.beginCombo(label, fontCurrent.debugName)) {
            ImGui.io.fonts.fonts.forEachIndexed { idx, font ->
                pushID(font)
                if (ImGui.selectable(font.debugName, font === fontCurrent)) {
                    ImGui.io.fontDefault = font
                    this.font = idx
                }
                popID()
            }
            ImGui.endCombo()
        }
    }
}

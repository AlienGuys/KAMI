package me.zeroeightsix.kami.gui.windows.modules

import glm_.vec2.Vec2
import imgui.*
import imgui.ImGui.acceptDragDropPayload
import imgui.ImGui.collapsingHeader
import imgui.ImGui.currentWindow
import imgui.ImGui.isItemClicked
import imgui.ImGui.setDragDropPayload
import imgui.ImGui.setNextWindowPos
import imgui.ImGui.text
import imgui.ImGui.treeNodeBehaviorIsOpen
import imgui.ImGui.treeNodeEx
import imgui.ImGui.treePop
import imgui.dsl.dragDropSource
import imgui.dsl.dragDropTarget
import imgui.dsl.menuItem
import imgui.dsl.popupContextItem
import imgui.dsl.window
import imgui.internal.ItemStatusFlag
import imgui.internal.or
import me.zeroeightsix.kami.gui.View.modulesOpen
import me.zeroeightsix.kami.gui.windows.GraphicalSettings
import me.zeroeightsix.kami.gui.windows.modules.Payloads.KAMI_MODULE_PAYLOAD
import me.zeroeightsix.kami.feature.FeatureManager
import me.zeroeightsix.kami.feature.module.Module

object Modules {

    internal var windows = getDefaultWindows()
    private val newWindows = mutableSetOf<ModuleWindow>()
    private val baseFlags = TreeNodeFlag.SpanFullWidth or TreeNodeFlag.OpenOnDoubleClick or TreeNodeFlag.NoTreePushOnOpen

    /**
     * Returns if this module has detached
     */
    fun collapsibleModule(
        module: Module,
        source: ModuleWindow,
        sourceGroup: String
    ): ModuleWindow? {
        val nodeFlags = if (!module.enabled) baseFlags else (baseFlags or TreeNodeFlag.Selected)
        val label = "${module.name}-node"
        var moduleWindow: ModuleWindow? = null

        // We don't want imgui to handle open/closing at all, so we hack out the behaviour
        val doubleClicked = ImGui.io.mouseDoubleClicked[0]
        ImGui.io.mouseDoubleClicked[0] = false

        var clickedLeft = false
        var clickedRight = false

        fun updateClicked() {
            clickedLeft = isItemClicked(if (GraphicalSettings.swapModuleListButtons) MouseButton.Left else MouseButton.Right)
            clickedRight = isItemClicked(if (GraphicalSettings.swapModuleListButtons) MouseButton.Right else MouseButton.Left)
        }

        val open = treeNodeEx(label, nodeFlags, module.name)
        dragDropTarget {
            acceptDragDropPayload(KAMI_MODULE_PAYLOAD)?.let {
                val payload = it.data!! as ModulePayload
                payload.moveTo(source, sourceGroup)
            }
        }
        if (open) {
            updateClicked()
            ModuleSettings(module) {
                dragDropSource(DragDropFlag.SourceAllowNullID.i) {
                    setDragDropPayload(KAMI_MODULE_PAYLOAD, ModulePayload(mutableSetOf(module), source))
                    text("Merge")
                }

                popupContextItem("$label-popup") {
                    menuItem("Detach") {
                        moduleWindow = ModuleWindow(module.name, module = module)
                    }
                }
            }

//            treePop()
        } else updateClicked()

        // Restore state
        ImGui.io.mouseDoubleClicked[0] = doubleClicked

        if (clickedLeft) {
            module.toggle()
        } else if (clickedRight) {
            val id = currentWindow.getID(label)
            val open = treeNodeBehaviorIsOpen(id, nodeFlags)
            val window = currentWindow
            window.dc.stateStorage[id] = !open
            window.dc.lastItemStatusFlags = window.dc.lastItemStatusFlags or ItemStatusFlag.ToggledOpen
        }
        
        return moduleWindow
    }

    operator fun invoke() {
        if (modulesOpen) {
            windows.removeIf(ModuleWindow::draw)
            if (windows.addAll(newWindows)) {
                newWindows.clear()
            }

            ModuleWindowsEditor()
        }
    }

    private fun getDefaultWindows() = mutableListOf(
        ModuleWindow("All modules", groups = FeatureManager.features.filterIsInstance<Module>().groupBy {
            it.category.getName()
        }.mapValuesTo(mutableMapOf(), { entry -> entry.value.toMutableList() }))
    )
    
    fun reset() {
        windows = getDefaultWindows()
    }

    class ModuleWindow(internal var title: String, val pos: Vec2? = null, var groups: Map<String, MutableList<Module>> = mapOf()) {

        constructor(title: String, pos: Vec2? = null, module: Module) : this(title, pos, mapOf(Pair("Group 1", mutableListOf(module))))

        var closed = false

        fun draw(): Boolean {
            pos?.let {
                setNextWindowPos(pos, Cond.Appearing)
            }
            
            fun iterateModules(list: MutableList<Module>, group: String): Boolean {
                return list.removeIf {
                    val moduleWindow = collapsibleModule(it, this, group)
                    moduleWindow?.let {
                        newWindows.add(moduleWindow)
                        return@removeIf true
                    }
                    return@removeIf false
                }
            }

            window("$title###${hashCode()}"){
                when {
                    groups.isEmpty() -> {
                        return true // close this window
                    }
                    groups.size == 1 -> {
                        val entry = groups.entries.stream().findAny().get()
                        val group = entry.value
                        if (group.isEmpty()) {
                            return true // close this window
                        }

                        iterateModules(group, entry.key)
                    }
                    else -> {
                        for ((group, list) in groups) {
                            if (list.isEmpty()) {
                                continue
                            }

                            if (collapsingHeader(group, TreeNodeFlag.SpanFullWidth.i)) {
                                iterateModules(list, group)
                            }
                        }
                    }
                }
            }

            return closed
        }

    }


}
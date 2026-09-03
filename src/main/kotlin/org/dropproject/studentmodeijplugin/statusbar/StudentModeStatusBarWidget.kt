package org.dropproject.studentmodeijplugin.statusbar

import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.Consumer
import java.awt.Component
import java.awt.event.MouseEvent

class StudentModeStatusBarWidget : StatusBarWidget, StatusBarWidget.TextPresentation {

    override fun ID(): String = StudentModeWidgetFactory.WIDGET_ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) {}

    override fun dispose() {}

    override fun getText(): String = "🔴 STUDENT MODE ON"

    override fun getAlignment(): Float = Component.CENTER_ALIGNMENT

    override fun getTooltipText(): String =
        "Student Mode is ON - AI assistance, code suggestions, quick fixes, and intention previews are disabled"

    override fun getClickConsumer(): Consumer<MouseEvent>? = null
}
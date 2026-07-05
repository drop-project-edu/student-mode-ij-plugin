package org.dropproject.studentmodeijplugin.statusbar

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import org.dropproject.studentmodeijplugin.services.StudentModeService

class StudentModeWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "Student Mode Indicator"

    override fun isAvailable(project: Project): Boolean =
        ApplicationManager.getApplication().getService(StudentModeService::class.java).isEnabled

    override fun createWidget(project: Project): StatusBarWidget = StudentModeStatusBarWidget()

    override fun disposeWidget(widget: StatusBarWidget) {
        widget.dispose()
    }

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    companion object {
        const val WIDGET_ID = "StudentModeStatusBarWidget"
    }
}
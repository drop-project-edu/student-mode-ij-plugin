package org.dropproject.studentmodeijplugin.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.dropproject.studentmodeijplugin.services.StudentModeService

/**
 * Removes the .noai file when a project window closes. The opening half is handled by
 * [org.dropproject.studentmodeijplugin.startup.StudentModeStartupActivity], because
 * ProjectManagerListener.projectOpened is scheduled for removal from the platform.
 */
class StudentModeProjectListener : ProjectManagerListener {

    override fun projectClosing(project: Project) {
        ApplicationManager.getApplication()
            .getService(StudentModeService::class.java)
            .onProjectClosing(project)
    }
}

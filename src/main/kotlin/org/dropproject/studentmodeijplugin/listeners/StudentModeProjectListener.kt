package org.dropproject.studentmodeijplugin.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.dropproject.studentmodeijplugin.services.StudentModeService

class StudentModeProjectListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        service().onProjectOpened(project)
    }

    override fun projectClosing(project: Project) {
        service().onProjectClosing(project)
    }

    private fun service(): StudentModeService =
        ApplicationManager.getApplication().getService(StudentModeService::class.java)
}
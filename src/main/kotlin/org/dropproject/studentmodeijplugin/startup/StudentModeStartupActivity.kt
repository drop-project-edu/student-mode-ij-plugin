package org.dropproject.studentmodeijplugin.startup

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.dropproject.studentmodeijplugin.services.StudentModeService

/**
 * Runs once for every project window that opens, including the ones restored at IDE startup, and
 * keeps the .noai file in sync with Student Mode. This replaces ProjectManagerListener.projectOpened,
 * which is scheduled for removal from the platform; the closing half still lives in
 * [org.dropproject.studentmodeijplugin.listeners.StudentModeProjectListener].
 */
class StudentModeStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication()
            .getService(StudentModeService::class.java)
            .onProjectOpened(project)
    }
}

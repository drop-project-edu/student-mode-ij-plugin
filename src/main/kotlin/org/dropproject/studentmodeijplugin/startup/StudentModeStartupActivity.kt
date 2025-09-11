package org.dropproject.studentmodeijplugin.startup

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.dropproject.studentmodeijplugin.services.StudentModeService

class StudentModeStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        val service = service<StudentModeService>()
        service.runStartupCheck()
    }
}

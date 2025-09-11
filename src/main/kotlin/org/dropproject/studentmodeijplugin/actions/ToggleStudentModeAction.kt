package org.dropproject.studentmodeijplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.dropproject.studentmodeijplugin.services.StudentModeService

class ToggleStudentModeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val service = ApplicationManager.getApplication().getService(StudentModeService::class.java)
        
        val newState = service.toggle()
        
        if (newState != null) {
            val message = if (newState) {
                "Student Mode is now ENABLED\n\n" +
                "Disabled Features:\n" +
                "• Quick fix light bulbs (red/yellow)\n" +
                "• Intention action previews\n\n" +
                "Check Settings > Editor > General > Appearance\n" +
                "to verify these are unchecked."
            } else {
                "Student Mode is now DISABLED\n\n" +
                "Restored Features:\n" +
                "• Quick fix light bulbs\n" +
                "• Intention action previews\n\n" +
                "Original settings restored."
            }
            
            Messages.showInfoMessage(project, message, "Student Mode")
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val service = ApplicationManager.getApplication().getService(StudentModeService::class.java)
        val isEnabled = service.isEnabled

        // Update text and icon based on current state
        e.presentation.text = if (isEnabled) {
            "Student Mode ON - All hints and AI help have been suppressed, use your brain"
        } else {
            "Normal Mode ON - Use all the power of hints and AI"
        }

        // Set different icons for ON/OFF states - GREEN = normal, RED = student mode active
        e.presentation.icon = if (isEnabled) {
            org.dropproject.studentmodeijplugin.StudentModeIcons.STUDENT_MODE_ON
        } else {
            org.dropproject.studentmodeijplugin.StudentModeIcons.STUDENT_MODE_OFF
        }
    }
}
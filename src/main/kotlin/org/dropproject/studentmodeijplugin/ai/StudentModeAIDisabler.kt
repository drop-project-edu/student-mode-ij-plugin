package org.dropproject.studentmodeijplugin.ai

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import org.dropproject.studentmodeijplugin.services.StudentModeService

class StudentModeAIDisabler : ProjectActivity {
    
    companion object {
        // Common AI-related action IDs that should be disabled
        private val AI_ACTION_IDS = listOf(
            "Github.Copilot.Actions.StartCopilot",
            "Github.Copilot.Actions.StopCopilot",
            "com.github.copilot.action.ShowCompletionsAction",
            "AIAssistant.Chat.OpenChatDialog",
            "AIAssistant.ContextActions",
            "OpenAI.ChatGPT.ToolWindow.Show",
            // Add more AI-related action IDs as needed
        )
    }
    
    override suspend fun execute(project: Project) {
        val studentModeService = project.service<StudentModeService>()
        val actionManager = ActionManager.getInstance()
        
        // Monitor for student mode changes and disable/enable AI actions accordingly
        monitorStudentModeChanges(project, actionManager, studentModeService)
    }
    
    private fun monitorStudentModeChanges(project: Project, actionManager: ActionManager, service: StudentModeService) {
        // For now, we'll check the state when actions are invoked
        // In a real implementation, you'd want to listen for state changes
        AI_ACTION_IDS.forEach { actionId ->
            val action = actionManager.getAction(actionId)
            if (action != null) {
                // We could wrap the action to check student mode before execution
                // This is a simplified approach - in practice you'd need more sophisticated handling
            }
        }
    }
}
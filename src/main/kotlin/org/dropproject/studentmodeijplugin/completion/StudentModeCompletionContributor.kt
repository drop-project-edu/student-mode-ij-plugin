package org.dropproject.studentmodeijplugin.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.openapi.components.service
import org.dropproject.studentmodeijplugin.services.StudentModeService

class StudentModeCompletionContributor : CompletionContributor() {
    
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val project = parameters.editor.project ?: return
        val studentModeService = project.service<StudentModeService>()
        
        if (studentModeService.isEnabled) {
            // When student mode is enabled, stop all completion
            result.stopHere()
            return
        }
        
        // When student mode is disabled, allow normal completion
        super.fillCompletionVariants(parameters, result)
    }
}
package org.dropproject.studentmodeijplugin.inspections

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import org.dropproject.studentmodeijplugin.services.StudentModeService

class StudentModeInspectionSuppressor : InspectionSuppressor {
    
    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        val project = element.project
        val studentModeService = project.service<StudentModeService>()
        
        // Suppress all inspections when student mode is enabled
        return studentModeService.isEnabled
    }
    
    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        // No suppress actions needed - we handle everything in isSuppressedFor
        return emptyArray()
    }
}

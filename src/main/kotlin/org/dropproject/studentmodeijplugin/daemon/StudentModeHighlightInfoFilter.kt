package org.dropproject.studentmodeijplugin.daemon

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import org.dropproject.studentmodeijplugin.services.StudentModeService

/**
 * Hides the IDE's own code suggestions while Student Mode is on, so a student is not handed
 * "immediately return this expression instead of assigning it to the temporary variable" for free.
 *
 * The daemon consults this filter as each [HighlightInfo] is built, so rejecting one removes the
 * whole trail at once: the squiggle, the hover tooltip, the gutter mark, the Problems view entry and
 * the Alt+Enter fix. It leaves the user's inspection profile alone, which means there is nothing to
 * capture and restore the way the editor settings in [StudentModeService] are - switching Student
 * Mode off simply stops the filtering, and the daemon restart the service already does on every
 * toggle is what repaints the editor.
 */
class StudentModeHighlightInfoFilter : HighlightInfoFilter {

    override fun accept(highlightInfo: HighlightInfo, file: PsiFile?): Boolean {
        if (!isStudentModeEnabled()) return true
        return isKept(highlightInfo.severity)
    }

    /**
     * Only the band of severities that carries suggestions is dropped: SERVER PROBLEM (100), INFO and
     * WEAK WARNING (200), WARNING (300), and any custom severity registered between them.
     *
     * The bands on either side have to survive. At or below TEXT_ATTRIBUTES (11) sits the daemon's
     * plain colouring - HighlightInfoType.SYMBOL_TYPE_SEVERITY, injected fragments, highlighted
     * references, and every silent annotation an annotator uses to paint a token - so filtering that
     * would strip syntax highlighting. At or above ERROR (400) sit real errors, which tell a student
     * their code is broken without telling them how to write it, plus ELEMENT_UNDER_CARET_SEVERITY
     * (401), which highlights the identifier under the caret.
     */
    private fun isKept(severity: HighlightSeverity): Boolean =
        severity <= HighlightSeverity.TEXT_ATTRIBUTES || severity >= HighlightSeverity.ERROR

    /**
     * This runs for every highlight in every pass, so it uses `getServiceIfCreated` to stay off the
     * hot path in an IDE where nothing has touched Student Mode yet. A service that does not exist
     * cannot have been switched on, which is the same answer as off.
     */
    private fun isStudentModeEnabled(): Boolean =
        ApplicationManager.getApplication()
            ?.getServiceIfCreated(StudentModeService::class.java)
            ?.isEnabled == true
}

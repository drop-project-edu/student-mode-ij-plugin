package org.dropproject.studentmodeijplugin

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.dropproject.studentmodeijplugin.daemon.StudentModeHighlightInfoFilter
import org.dropproject.studentmodeijplugin.services.StudentModeService

/**
 * Student Mode drops the daemon's suggestions but has to leave the two bands around them alone:
 * errors, so a student still sees that their code is broken, and the low severities the daemon uses
 * for plain colouring, so syntax highlighting survives.
 */
class StudentModeHighlightFilterTest : BasePlatformTestCase() {

    private lateinit var service: StudentModeService
    private val filter = StudentModeHighlightInfoFilter()

    override fun setUp() {
        super.setUp()
        service = ApplicationManager.getApplication().getService(StudentModeService::class.java)
    }

    override fun tearDown() {
        try {
            service.setEnabled(false)
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun testKeepsSuggestionsWhileStudentModeIsOff() {
        assertTrue("a warning must be shown in Normal Mode", accepts(HighlightInfoType.WARNING))
        assertTrue("a weak warning must be shown in Normal Mode", accepts(HighlightInfoType.WEAK_WARNING))
    }

    fun testDropsSuggestionsWhileStudentModeIsOn() {
        service.setEnabled(true)

        assertFalse("a warning is a suggestion", accepts(HighlightInfoType.WARNING))
        assertFalse("a weak warning is a suggestion", accepts(HighlightInfoType.WEAK_WARNING))
        assertFalse(
            "an external annotator's problem is a suggestion",
            accepts(HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER)
        )
    }

    fun testKeepsErrorsWhileStudentModeIsOn() {
        service.setEnabled(true)

        assertTrue("a compile error must stay visible", accepts(HighlightInfoType.ERROR))
        assertTrue("an unresolved reference must stay visible", accepts(HighlightInfoType.WRONG_REF))
        assertTrue(
            "the identifier under the caret is highlighted above ERROR",
            accepts(HighlightInfoType.ELEMENT_UNDER_CARET_READ)
        )
    }

    fun testKeepsPlainColouringWhileStudentModeIsOn() {
        service.setEnabled(true)

        assertTrue("token colouring must survive", accepts(HighlightInfoType.TEXT_ATTRIBUTES))
        assertTrue("information-level highlights must survive", accepts(HighlightInfoType.INFORMATION))
        assertTrue("injected fragments must survive", accepts(HighlightInfoType.INJECTED_LANGUAGE_FRAGMENT))
    }

    /** Toggling back off has to restore suggestions without any stored state to restore from. */
    fun testSuggestionsComeBackWhenStudentModeGoesOff() {
        service.setEnabled(true)
        service.setEnabled(false)

        assertTrue("a warning must be shown again", accepts(HighlightInfoType.WARNING))
    }

    private fun accepts(type: HighlightInfoType): Boolean {
        val info = HighlightInfo.newHighlightInfo(type)
            .range(0, 1)
            .descriptionAndTooltip("suggestion")
            .createUnconditionally()
        return filter.accept(info, null)
    }
}

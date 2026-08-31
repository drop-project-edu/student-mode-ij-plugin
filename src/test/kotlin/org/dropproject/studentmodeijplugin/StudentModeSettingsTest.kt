package org.dropproject.studentmodeijplugin

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.dropproject.studentmodeijplugin.services.StudentModeService

/**
 * Student Mode captures the user's editor settings when it is switched on and puts them back when it
 * is switched off. These tests pin down that pairing: what comes back has to be what was captured,
 * not a hardcoded default.
 */
class StudentModeSettingsTest : BasePlatformTestCase() {

    private lateinit var service: StudentModeService
    private lateinit var editorSettings: EditorSettingsExternalizable

    private var originalBulb = true
    private var originalPreview = true
    private var originalTooltips = true

    override fun setUp() {
        super.setUp()
        service = ApplicationManager.getApplication().getService(StudentModeService::class.java)
        editorSettings = EditorSettingsExternalizable.getInstance()

        // These are application-level settings shared with the rest of the test run, so remember the
        // real values and put them back in tearDown().
        originalBulb = editorSettings.isShowIntentionBulb
        originalPreview = editorSettings.isShowIntentionPreview
        originalTooltips = PropertiesComponent.getInstance().getBoolean(TOOLTIP_ACTIONS_KEY, true)
    }

    override fun tearDown() {
        try {
            service.setEnabled(false)
            editorSettings.isShowIntentionBulb = originalBulb
            editorSettings.isShowIntentionPreview = originalPreview
            PropertiesComponent.getInstance().setValue(TOOLTIP_ACTIONS_KEY, originalTooltips, true)
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }

    fun testCapturesSettingsThatAreOn() {
        editorSettings.isShowIntentionBulb = true
        editorSettings.isShowIntentionPreview = true

        service.setEnabled(true)

        assertTrue("intention bulb should be captured as on", service.getState().originalQuickFixes)
        assertTrue("intention preview should be captured as on", service.getState().originalIntentionPreview)
        assertFalse("intention bulb should be switched off", editorSettings.isShowIntentionBulb)
        assertFalse("intention preview should be switched off", editorSettings.isShowIntentionPreview)
    }

    fun testCapturesSettingsThatAreAlreadyOff() {
        editorSettings.isShowIntentionBulb = false
        editorSettings.isShowIntentionPreview = false

        service.setEnabled(true)

        assertFalse("intention bulb should be captured as off", service.getState().originalQuickFixes)
        assertFalse("intention preview should be captured as off", service.getState().originalIntentionPreview)
    }

    fun testRestoresSettingsThatWereOn() {
        editorSettings.isShowIntentionBulb = true
        editorSettings.isShowIntentionPreview = true

        service.setEnabled(true)
        service.setEnabled(false)

        assertTrue("intention bulb should be back on", editorSettings.isShowIntentionBulb)
        assertTrue("intention preview should be back on", editorSettings.isShowIntentionPreview)
    }

    /**
     * The failure mode behind the 0.2.0 restore bug: when capturing falls back to a default of "on",
     * switching Student Mode off turns the bulb back on for someone who had deliberately turned it off.
     */
    fun testRestoresSettingsThatWereAlreadyOff() {
        editorSettings.isShowIntentionBulb = false
        editorSettings.isShowIntentionPreview = false

        service.setEnabled(true)
        service.setEnabled(false)

        assertFalse("a bulb the user had turned off must stay off", editorSettings.isShowIntentionBulb)
        assertFalse("a preview the user had turned off must stay off", editorSettings.isShowIntentionPreview)
    }

    /** Every enable re-captures, so a setting changed between toggles is not restored from a stale value. */
    fun testRecapturesOnEveryEnable() {
        editorSettings.isShowIntentionBulb = true
        service.setEnabled(true)
        service.setEnabled(false)
        assertTrue("first cycle should restore the bulb to on", editorSettings.isShowIntentionBulb)

        // The user turns the bulb off while Student Mode is off.
        editorSettings.isShowIntentionBulb = false

        service.setEnabled(true)
        service.setEnabled(false)
        assertFalse("the second enable should have captured the newer value", editorSettings.isShowIntentionBulb)
    }

    fun testRestoresTooltipActions() {
        val properties = PropertiesComponent.getInstance()
        properties.setValue(TOOLTIP_ACTIONS_KEY, false, true)

        service.setEnabled(true)

        assertFalse("tooltip actions should be captured as off", service.getState().originalTooltipActions)
        assertFalse("tooltip actions should be switched off", properties.getBoolean(TOOLTIP_ACTIONS_KEY, true))

        service.setEnabled(false)

        assertFalse(
            "a tooltip setting the user had turned off must stay off",
            properties.getBoolean(TOOLTIP_ACTIONS_KEY, true)
        )
    }

    private companion object {
        // Mirrors the key StudentModeService writes to.
        const val TOOLTIP_ACTIONS_KEY = "tooltips.show.actions.in.key"
    }
}

package org.dropproject.studentmodeijplugin.services

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.project.ProjectManager

@Service
@State(
    name = "org.dropproject.studentmodeijplugin.services.StudentModeService",
    storages = [Storage("StudentMode.xml")]
)
class StudentModeService : PersistentStateComponent<StudentModeService.PluginState>, Disposable {

    data class PluginState(
        var isEnabled: Boolean = false,
        var originalQuickFixes: Boolean = true,
        var originalIntentionPreview: Boolean = true,
        var originalTooltipActions: Boolean = true
    )

    private var state = PluginState()
    private val logger = thisLogger()

    override fun getState(): PluginState {
        return state
    }

    override fun loadState(state: PluginState) {
        this.state = state
    }

    val isEnabled: Boolean get() = state.isEnabled

    fun toggle(): Boolean {
        val newState = !state.isEnabled
        if (newState) {
            enableStudentMode()
        } else {
            disableStudentMode()
        }
        state.isEnabled = newState
        logger.info("Student Mode ${if (newState) "enabled" else "disabled"} globally")
        return newState
    }

    private fun enableStudentMode() {
        logger.info("Enabling Student Mode - capturing current settings and disabling features")

        // ALWAYS capture current settings before disabling (user might have changed them)
        state.originalQuickFixes = getQuickFixesEnabled()
        state.originalIntentionPreview = getIntentionPreviewEnabled()
        val properties = PropertiesComponent.getInstance()
        state.originalTooltipActions = properties.getBoolean("tooltips.show.actions.in.key", true)
        logger.info("Current settings captured: quickFixes=${state.originalQuickFixes}, preview=${state.originalIntentionPreview}, tooltips=${state.originalTooltipActions}")

        // Disable features
        disableQuickFixes()
        properties.setValue("tooltips.show.actions.in.key", false, true)

        // Refresh code analysis for all open projects
        ProjectManager.getInstance().openProjects.forEach { project ->
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    private fun disableStudentMode() {
        logger.info("Disabling Student Mode - restoring original settings")

        // Restore original settings
        setQuickFixesEnabled(state.originalQuickFixes)
        setIntentionPreviewEnabled(state.originalIntentionPreview)
        val properties = PropertiesComponent.getInstance()
        properties.setValue("tooltips.show.actions.in.key", state.originalTooltipActions, true)

        logger.info("Restored settings: quickFixes=${state.originalQuickFixes}, preview=${state.originalIntentionPreview}, tooltips=${state.originalTooltipActions}")

        // Refresh code analysis for all open projects
        ProjectManager.getInstance().openProjects.forEach { project ->
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }

    private fun disableQuickFixes() {
        try {
            // Use editor settings to disable intention bulb and preview
            val editorSettings = EditorSettingsExternalizable.getInstance()
            editorSettings.isShowIntentionBulb = false
            editorSettings.isShowIntentionPreview = false
            
            logger.info("Disabled quick fix intentions and preview via settings")
        } catch (e: Exception) {
            logger.warn("Could not disable quick fix intentions", e)
        }
    }

    private fun getQuickFixesEnabled(): Boolean {
        return try {
            EditorSettingsExternalizable.getInstance().isShowIntentionBulb
        } catch (e: Exception) {
            true
        }
    }

    private fun getIntentionPreviewEnabled(): Boolean {
        return try {
            EditorSettingsExternalizable.getInstance().isShowIntentionPreview
        } catch (e: Exception) {
            true
        }
    }

    private fun setQuickFixesEnabled(enabled: Boolean) {
        try {
            val editorSettings = EditorSettingsExternalizable.getInstance()
            if (editorSettings != null) {
                editorSettings.isShowIntentionBulb = enabled
                logger.info("Set intention bulb enabled: $enabled")
            } else {
                logger.warn("Couldn't set intention bulb state because EditorSettingsExternalizable is null")
            }
        } catch (e: Exception) {
            logger.warn("Could not set intention bulb state", e)
        }
    }

    private fun setIntentionPreviewEnabled(enabled: Boolean) {
        try {
            val editorSettings = EditorSettingsExternalizable.getInstance()
            if (editorSettings != null) {
                editorSettings.isShowIntentionPreview = enabled
                logger.info("Set intention preview enabled: $enabled")
            } else {
                logger.warn("Couldn't set intention preview state because EditorSettingsExternalizable is null")
            }
        } catch (e: Exception) {
            logger.warn("Could not set intention preview state", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled != state.isEnabled) {
            toggle()
        }
    }

    override fun dispose() {
        logger.info("StudentModeService disposing - checking if Student Mode needs cleanup")
        if (state.isEnabled) {
            logger.info("Student Mode was ON during shutdown - reverting settings")
            disableStudentMode()
        }
    }

    fun runStartupCheck() {
        if (state.isEnabled) {
            logger.info("Student Mode was left ON. Restoring settings and disabling.")
            disableStudentMode()
            state.isEnabled = false
        }
    }
}

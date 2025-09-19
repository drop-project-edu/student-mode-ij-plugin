package org.dropproject.studentmodeijplugin.services

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

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
    
    // File monitoring for .noai files per project
    private val projectNoaiFiles = ConcurrentHashMap<String, File>()
    private val monitoringService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "StudentMode-NoAI-Monitor").apply { isDaemon = true }
    }
    private var monitoringTask: ScheduledFuture<*>? = null
    
    companion object {
        private const val NOAI_FILENAME = ".noai"
        private const val NOAI_CONTENT = "Created and managed by Student Mode plugin. Don't remove."
        private const val MONITORING_INTERVAL_SECONDS = 5L
    }

    override fun getState(): PluginState {
        return state
    }

    override fun loadState(state: PluginState) {
        this.state = state
    }

    val isEnabled: Boolean get() = state.isEnabled

    fun toggle(project: Project? = null): Boolean? {
        val newState = !state.isEnabled
        if (newState) {
            val activeAIPlugins = checkForActiveAIPlugins()
            if (activeAIPlugins.isNotEmpty()) {
                val pluginList = activeAIPlugins.joinToString(", ")
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Student Mode Notifications")
                    .createNotification(
                        "Student Mode cannot be enabled",
                        "Please disable the following AI plugins first: $pluginList",
                        NotificationType.ERROR
                    )
                    .notify(project)
                return null // State was not changed
            }
            enableStudentMode()
            project?.let { createNoAiFile(it) }
            startMonitoring()
        } else {
            disableStudentMode()
            project?.let { removeNoAiFile(it) }
            stopMonitoring()
        }
        state.isEnabled = newState
        logger.info("Student Mode ${if (newState) "enabled" else "disabled"} globally")
        return newState
    }

    private fun checkForActiveAIPlugins(): List<String> {
        val aiPluginIds = listOf(
            "com.github.copilot",  // github copilot
            "com.google.tools.ij.aiplugin",  // gemini code assist
            "com.ai.engine.cty" // deepseek AI developer
        )
        val activeAIPlugins = mutableListOf<String>()
        for (pluginId in aiPluginIds) {
            val plugin = PluginManagerCore.getPlugin(PluginId.getId(pluginId))
            if (plugin != null && plugin.isEnabled) {
                activeAIPlugins.add(plugin.name)
            }
        }
        return activeAIPlugins
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

    fun setEnabled(enabled: Boolean, project: Project? = null) {
        if (enabled != state.isEnabled) {
            toggle(project)
        }
    }
    
    private fun createNoAiFile(project: Project) {
        try {
            val projectPath = project.basePath ?: return
            val noaiFile = File(projectPath, NOAI_FILENAME)
            
            if (!noaiFile.exists()) {
                noaiFile.writeText(NOAI_CONTENT)
                logger.info("Created .noai file at: ${noaiFile.absolutePath}")
                
                // Store the file reference for monitoring
                projectNoaiFiles[project.locationHash] = noaiFile
                
                // Refresh the VFS to make the file visible in IDE
                ApplicationManager.getApplication().invokeLater {
                    LocalFileSystem.getInstance().refreshAndFindFileByPath(noaiFile.absolutePath)
                }
                
                // Note: Removed notification about .noai file creation per user request
            }
        } catch (e: Exception) {
            logger.warn("Failed to create .noai file for project: ${project.name}", e)
        }
    }
    
    private fun removeNoAiFile(project: Project) {
        try {
            val projectPath = project.basePath ?: return
            val noaiFile = File(projectPath, NOAI_FILENAME)
            
            if (noaiFile.exists()) {
                val deleted = noaiFile.delete()
                if (deleted) {
                    logger.info("Removed .noai file from: ${noaiFile.absolutePath}")
                } else {
                    logger.warn("Failed to delete .noai file: ${noaiFile.absolutePath}")
                }
            }
            
            // Remove from monitoring
            projectNoaiFiles.remove(project.locationHash)
            
            // Refresh the VFS
            ApplicationManager.getApplication().invokeLater {
                LocalFileSystem.getInstance().refreshAndFindFileByPath(noaiFile.absolutePath)
            }
        } catch (e: Exception) {
            logger.warn("Failed to remove .noai file for project: ${project.name}", e)
        }
    }
    
    private fun startMonitoring() {
        if (monitoringTask?.isCancelled != false) {
            monitoringTask = monitoringService.scheduleWithFixedDelay(
                ::checkNoAiFiles,
                MONITORING_INTERVAL_SECONDS,
                MONITORING_INTERVAL_SECONDS,
                TimeUnit.SECONDS
            )
            logger.info("Started .noai file monitoring")
        }
    }
    
    private fun stopMonitoring() {
        monitoringTask?.cancel(false)
        monitoringTask = null
        logger.info("Stopped .noai file monitoring")
    }
    
    private fun checkNoAiFiles() {
        if (!state.isEnabled) return
        
        val iterator = projectNoaiFiles.entries.iterator()
        while (iterator.hasNext()) {
            val (projectHash, noaiFile) = iterator.next()
            
            var shouldDisable = false
            var reason = ""
            
            if (!noaiFile.exists()) {
                shouldDisable = true
                reason = "The .noai file was manually deleted."
                logger.warn(".noai file was manually deleted: ${noaiFile.absolutePath}")
            } else {
                // Check if content was modified
                try {
                    val content = noaiFile.readText().trim()
                    if (content != NOAI_CONTENT) {
                        shouldDisable = true
                        reason = "The .noai file content was modified."
                        logger.warn(".noai file content was modified: ${noaiFile.absolutePath}")
                    }
                } catch (e: Exception) {
                    shouldDisable = true
                    reason = "The .noai file could not be read."
                    logger.warn("Failed to read .noai file: ${noaiFile.absolutePath}", e)
                }
            }
            
            if (shouldDisable) {
                // Find the project by hash
                val project = ProjectManager.getInstance().openProjects.find { it.locationHash == projectHash }
                
                if (project != null) {
                    // Disable student mode and notify user on EDT
                    ApplicationManager.getApplication().invokeLater {
                        state.isEnabled = false
                        disableStudentMode()
                        
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("Student Mode Notifications")
                            .createNotification(
                                "Student Mode Disabled",
                                "$reason Student Mode has been automatically disabled.",
                                NotificationType.WARNING
                            )
                            .notify(project)
                        
                        logger.info("Student Mode automatically disabled due to .noai file issue: $reason")
                    }
                }
                
                iterator.remove()
            }
        }
        
        // If no more files to monitor, stop monitoring
        if (projectNoaiFiles.isEmpty()) {
            stopMonitoring()
        }
    }

    override fun dispose() {
        logger.info("StudentModeService disposing - checking if Student Mode needs cleanup")
        
        // Stop monitoring and clean up
        stopMonitoring()
        monitoringService.shutdown()
        
        if (state.isEnabled) {
            logger.info("Student Mode was ON during shutdown - reverting settings")
            disableStudentMode()
            
            // Clean up .noai files
            projectNoaiFiles.values.forEach { file ->
                try {
                    if (file.exists()) {
                        file.delete()
                        logger.info("Cleaned up .noai file: ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to clean up .noai file: ${file.absolutePath}", e)
                }
            }
        }
        
        projectNoaiFiles.clear()
    }

    fun runStartupCheck() {
        if (state.isEnabled) {
            logger.info("Student Mode was left ON. Restoring settings and disabling.")
            disableStudentMode()
            state.isEnabled = false
        }
        
        // Always clean up any .noai files from previous sessions on startup
        ProjectManager.getInstance().openProjects.forEach { project ->
            cleanupNoAiFileOnStartup(project)
        }
    }
    
    private fun cleanupNoAiFileOnStartup(project: Project) {
        try {
            val projectPath = project.basePath ?: return
            val noaiFile = File(projectPath, NOAI_FILENAME)
            
            if (noaiFile.exists()) {
                // Check if it's our file by reading its content
                val content = try {
                    noaiFile.readText().trim()
                } catch (e: Exception) {
                    logger.warn("Failed to read .noai file content: ${noaiFile.absolutePath}", e)
                    return
                }
                
                if (content == NOAI_CONTENT) {
                    val deleted = noaiFile.delete()
                    if (deleted) {
                        logger.info("Cleaned up Student Mode .noai file on startup: ${noaiFile.absolutePath}")
                    } else {
                        logger.warn("Failed to clean up .noai file on startup: ${noaiFile.absolutePath}")
                    }
                    
                    // Refresh the VFS
                    ApplicationManager.getApplication().invokeLater {
                        LocalFileSystem.getInstance().refreshAndFindFileByPath(noaiFile.absolutePath)
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to clean up .noai file on startup for project: ${project.name}", e)
        }
    }
}
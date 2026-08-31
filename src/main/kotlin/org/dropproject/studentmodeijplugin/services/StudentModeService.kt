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
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import org.dropproject.studentmodeijplugin.statusbar.StudentModeWidgetFactory
import org.jetbrains.completion.full.line.settings.FullLineSettings
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service(Service.Level.APP)
@State(
    name = "org.dropproject.studentmodeijplugin.services.StudentModeService",
    storages = [Storage("StudentMode.xml")]
)
class StudentModeService : PersistentStateComponent<StudentModeService.PluginState>, Disposable {

    data class PluginState(
        var isEnabled: Boolean = false,
        var originalQuickFixes: Boolean = true,
        var originalIntentionPreview: Boolean = true,
        var originalTooltipActions: Boolean = true,
        var originalFullLineCompletionEnabled: Boolean = true
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
        private const val ML_COMPLETION_SETTINGS_CLASS =
            "com.intellij.ml.inline.completion.impl.configuration.MLCompletionSettings"

        /** Newest name first: 262 renamed 261's setInlineCompletionEnabled to setCompletionEnabled. */
        private val ML_COMPLETION_SETTERS = listOf("setCompletionEnabled", "setInlineCompletionEnabled")
    }

    override fun getState(): PluginState {
        return state
    }

    override fun loadState(state: PluginState) {
        this.state = state
        if (state.isEnabled) {
            // A new IDE session always starts in Normal Mode. If this is still true here,
            // the previous session ended abnormally (crash/force-quit) while Student Mode
            // was on, so restore the settings it changed before resetting the flag.
            logger.info("Student Mode was left ON from a previous session - restoring settings and resetting to OFF")
            disableStudentMode()
            this.state.isEnabled = false
        }
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
            ProjectManager.getInstance().openProjects.forEach { createNoAiFile(it) }
            startMonitoring()
        } else {
            disableStudentMode()
            ProjectManager.getInstance().openProjects.forEach { removeNoAiFile(it) }
            stopMonitoring()
        }
        state.isEnabled = newState
        logger.info("Student Mode ${if (newState) "enabled" else "disabled"} globally")
        refreshStatusBarWidgets()
        return newState
    }

    private fun refreshStatusBarWidgets() {
        ProjectManager.getInstance().openProjects.forEach { project ->
            project.getService(StatusBarWidgetsManager::class.java).updateWidget(StudentModeWidgetFactory::class.java)
        }
    }

    /** Called when a project window finishes opening, for every project (including ones restored at IDE startup). */
    fun onProjectOpened(project: Project) {
        if (state.isEnabled) {
            createNoAiFile(project)
            startMonitoring()
        } else {
            cleanupStrayNoAiFile(project)
        }
    }

    /** Called when a specific project window is closing, independent of other open windows or app shutdown. */
    fun onProjectClosing(project: Project) {
        if (state.isEnabled) {
            removeNoAiFile(project)
        }
    }

    private fun checkForActiveAIPlugins(): List<String> {
        // Display names are hardcoded (rather than read off the plugin descriptor) so this check only
        // needs the public PluginManagerCore.isLoaded(PluginId), not the internal getPlugin(PluginId).
        val aiPlugins = mapOf(
            "com.github.copilot" to "GitHub Copilot",
            "com.google.tools.ij.aiplugin" to "Gemini Code Assist",
            "com.ai.engine.cty" to "DeepSeek AI Developer"
        )
        return aiPlugins.filterKeys { PluginManagerCore.isLoaded(PluginId.getId(it)) }.values.toList()
    }

    private fun enableStudentMode() {
        logger.info("Enabling Student Mode - capturing current settings and disabling features")

        // ALWAYS capture current settings before disabling (user might have changed them)
        state.originalQuickFixes = getQuickFixesEnabled()
        state.originalIntentionPreview = getIntentionPreviewEnabled()
        val properties = PropertiesComponent.getInstance()
        state.originalTooltipActions = properties.getBoolean("tooltips.show.actions.in.key", true)
        state.originalFullLineCompletionEnabled = getFullLineCompletionEnabled()
        logger.info("Current settings captured: quickFixes=${state.originalQuickFixes}, preview=${state.originalIntentionPreview}, tooltips=${state.originalTooltipActions}, fullLineCompletion=${state.originalFullLineCompletionEnabled}")

        // Disable features
        disableQuickFixes()
        properties.setValue("tooltips.show.actions.in.key", false, true)
        setFullLineCompletionEnabled(false)

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
        setFullLineCompletionEnabled(state.originalFullLineCompletionEnabled)

        logger.info("Restored settings: quickFixes=${state.originalQuickFixes}, preview=${state.originalIntentionPreview}, tooltips=${state.originalTooltipActions}, fullLineCompletion=${state.originalFullLineCompletionEnabled}")

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
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not disable quick fix intentions", e)
        }
    }

    private fun getQuickFixesEnabled(): Boolean {
        return try {
            EditorSettingsExternalizable.getInstance().isShowIntentionBulb
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            true
        }
    }

    private fun getIntentionPreviewEnabled(): Boolean {
        return try {
            EditorSettingsExternalizable.getInstance().isShowIntentionPreview
        } catch (e: ProcessCanceledException) {
            throw e
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
        } catch (e: ProcessCanceledException) {
            throw e
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
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not set intention preview state", e)
        }
    }

    /**
     * The inline completion checkbox (Editor > General > Inline Completion) is backed by the bundled
     * "Full Line Code Completion" plugin, declared as an optional dependency in plugin.xml so its classes
     * load. If that plugin is absent or disabled, reaching for them fails, which is swallowed here -
     * Student Mode should keep working for everything else regardless.
     *
     * Build 262 moved the setting to MLCompletionSettings.completionEnabled and deprecated
     * FullLineSettings.settingsState.enable, which still accepts writes there but no longer drives the
     * feature. Builds below 262, which this plugin still supports, ship MLCompletionSettings without that
     * method, so it is called reflectively: a direct call compiles, but the plugin verifier then reports an
     * unresolved method against every one of those builds.
     */
    private fun getFullLineCompletionEnabled(): Boolean {
        mlCompletionSettings()?.let { settings ->
            readMlCompletionEnabled(settings)?.let { return it }
        }
        return try {
            @Suppress("DEPRECATION")
            FullLineSettings.getInstance().settingsState.enable
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            logger.warn("Could not read inline completion enabled state", e)
            true
        }
    }

    private fun setFullLineCompletionEnabled(enabled: Boolean) {
        val settings = mlCompletionSettings()
        if (settings != null && writeMlCompletionEnabled(settings, enabled)) {
            logger.info("Set inline completion enabled via MLCompletionSettings: $enabled")
            return
        }
        try {
            @Suppress("DEPRECATION")
            FullLineSettings.getInstance().settingsState.enable = enabled
            logger.info("Set inline completion enabled via FullLineSettings: $enabled")
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            logger.warn("Could not set inline completion enabled state", e)
        }
    }

    /** The MLCompletionSettings service on IDE builds that ship it, or null on the ones that do not. */
    private fun mlCompletionSettings(): Any? =
        try {
            val settingsClass = Class.forName(ML_COMPLETION_SETTINGS_CLASS, true, javaClass.classLoader)
            ApplicationManager.getApplication().getService(settingsClass)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Throwable) {
            logger.debug("MLCompletionSettings is not available on this IDE build", e)
            null
        }

    /**
     * 262 exposes a getter on the settings object itself; 261 only exposes one on its state object.
     * Builds at or below 253 have neither, and are handled by the FullLineSettings fallback.
     */
    private fun readMlCompletionEnabled(settings: Any): Boolean? {
        try {
            return settings.javaClass.getMethod("isCompletionEnabled").invoke(settings) as Boolean
        } catch (e: NoSuchMethodException) {
            // 261 and older: try the state object below.
        } catch (e: InvocationTargetException) {
            (e.targetException as? ProcessCanceledException)?.let { throw it }
            logger.warn("Could not read MLCompletionSettings.completionEnabled", e)
            return null
        } catch (e: Throwable) {
            logger.debug("Could not read MLCompletionSettings.completionEnabled", e)
            return null
        }
        return try {
            val state = settings.javaClass.getMethod("getState").invoke(settings) ?: return null
            state.javaClass.getMethod("getCompletionEnabled").invoke(state) as Boolean
        } catch (e: InvocationTargetException) {
            (e.targetException as? ProcessCanceledException)?.let { throw it }
            logger.warn("Could not read MLCompletionSettings state", e)
            null
        } catch (e: Throwable) {
            logger.debug("MLCompletionSettings state has no completionEnabled on this IDE build", e)
            null
        }
    }

    /**
     * The setter was renamed between builds: setInlineCompletionEnabled on 261, setCompletionEnabled on
     * 262. Both notify listeners, so they are preferred over writing the state object directly.
     */
    private fun writeMlCompletionEnabled(settings: Any, enabled: Boolean): Boolean {
        for (name in ML_COMPLETION_SETTERS) {
            try {
                settings.javaClass
                    .getMethod(name, Boolean::class.javaPrimitiveType)
                    .invoke(settings, enabled)
                return true
            } catch (e: NoSuchMethodException) {
                continue
            } catch (e: InvocationTargetException) {
                (e.targetException as? ProcessCanceledException)?.let { throw it }
                logger.warn("Could not call MLCompletionSettings.$name", e)
                return false
            } catch (e: Throwable) {
                logger.debug("Could not call MLCompletionSettings.$name", e)
                return false
            }
        }
        logger.debug("MLCompletionSettings has no known enable setter on this IDE build")
        return false
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
                        refreshStatusBarWidgets()

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
            // App services are already disposed during IDE shutdown, so restoring here throws.
            // state.isEnabled stays true on disk and loadState() restores on the next start.
            val application = ApplicationManager.getApplication()
            if (application != null && !application.isDisposed) {
                logger.info("Student Mode was ON while the plugin was unloaded - restoring settings now")
                disableStudentMode()
                state.isEnabled = false
            } else {
                logger.info("Student Mode was ON during IDE shutdown - settings will be restored on the next start")
            }

            // Plain file I/O, safe whether or not the application is still up.
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

    private fun cleanupStrayNoAiFile(project: Project) {
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
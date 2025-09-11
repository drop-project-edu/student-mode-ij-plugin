package org.dropproject.studentmodeijplugin

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object StudentModeIcons {
    @JvmField
    val STUDENT_MODE_OFF: Icon = IconLoader.getIcon("/icons/studentModeOff.svg", javaClass)
    
    @JvmField
    val STUDENT_MODE_ON: Icon = IconLoader.getIcon("/icons/studentModeOn.svg", javaClass)
}
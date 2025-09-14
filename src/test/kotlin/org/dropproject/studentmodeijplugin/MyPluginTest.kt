package org.dropproject.studentmodeijplugin

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import org.dropproject.studentmodeijplugin.services.StudentModeService

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    fun testStudentModeService() {
        val studentModeService = project.service<StudentModeService>()

        // Test default state is OFF
        assertFalse(studentModeService.isEnabled)

        // Test toggle functionality
        assertEquals(true, studentModeService.toggle())
        assertTrue(studentModeService.isEnabled)

        assertEquals(true, studentModeService.toggle())
        assertFalse(studentModeService.isEnabled)
    }

    fun testStudentModeSetEnabled() {
        val studentModeService = project.service<StudentModeService>()

        // Test setting enabled state directly
        studentModeService.setEnabled(true)
        assertTrue(studentModeService.isEnabled)

        studentModeService.setEnabled(false)
        assertFalse(studentModeService.isEnabled)
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}

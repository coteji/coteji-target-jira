package io.github.coteji.tests

import io.github.coteji.clients.JiraClient
import io.github.coteji.model.CotejiTest
import io.github.coteji.targets.JiraTarget
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JiraTargetTest {
    private val tests = listOf(
        CotejiTest(
            id = "COT-8",
            name = "[TEST] Create Reminder",
            content = "Open Reminders App\nAdd Reminder [ reminder ]\nCheck Last Reminder [ reminder ]",
            attributes = mapOf()
        ),
        CotejiTest(
            id = "COT-9",
            name = "[TEST] Delete Reminder",
            content = "Open Reminders App\n" +
                    "Add Reminder [ reminder ]\n" +
                    "Delete Last Reminder\n" +
                    "Refresh Page\n" +
                    "Check Reminder Is Absent [ reminder ]",
            attributes = mapOf()
        )
    )

    private val jira = JiraTarget(
        baseUrl = "https://coteji.atlassian.net",
        userName = "maxbarvinskyi@gmail.com",
        project = "COT",
        testIssueType = "Test Case"
    )

    init {
        (jira.jiraClient as JiraClient).issuesChunkSize = 1
    }

    @Test
    fun `dry run no-force`() {
        val result = jira.dryRun(tests, false)
        assertThat(result.testsAdded).isEmpty()
        assertThat(result.testsUpdated).isEmpty()
        assertThat(result.testsAlreadyUpToDate).hasSize(2)
        assertThat(result.testsDeleted).isEmpty()
        assertThat(result.testsSyncFailed).isEmpty()
    }

    @Test
    fun `dry run force`() {
        val result = jira.dryRun(tests, true)
        assertThat(result.testsAdded).isEmpty()
        assertThat(result.testsUpdated).hasSize(2)
        assertThat(result.testsAlreadyUpToDate).isEmpty()
        assertThat(result.testsDeleted).isEmpty()
        assertThat(result.testsSyncFailed).isEmpty()
    }

}
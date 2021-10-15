package io.github.coteji.tests

import io.github.coteji.clients.JiraClient
import io.github.coteji.clients.JiraClientException
import io.github.coteji.model.CotejiTest
import io.github.coteji.targets.toCreatePayload
import io.github.coteji.targets.toEditPayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class JiraClientTest {
    private val test = CotejiTest(
        name = "[TEST-CLIENT] Jira Client Test",
        content = "First Row\nSecond Row",
        attributes = mapOf(Pair("labels", listOf("client", "api")))
    )
    private val updatedTest = CotejiTest(
        name = "[TEST-CLIENT] Jira Client Test 2",
        content = "First Row 1\nSecond Row",
        attributes = mapOf(Pair("labels", listOf("new")))
    )
    private val jiraClient = JiraClient("https://coteji.atlassian.net", "maxbarvinskyi@gmail.com")

    @Test
    fun `crud test`() {
        val projectId = jiraClient.getProjectId("COT")
        val issueTypeId = jiraClient.getIssueTypeId("Test Case", projectId)

        // create
        val addedIds = jiraClient.createIssues(listOf(test.toCreatePayload(projectId, issueTypeId)))
        assertThat(addedIds).hasSize(1)
        val key = addedIds[0]

        // read
        var issue = jiraClient.getIssue(key)
        var fields = issue["fields"] as Map<String, Any>
        assertThat(fields["summary"]).isEqualTo(test.name)
        assertThat(fields["description"]).isEqualTo(test.content)
        assertThat(fields["labels"] as List<String>).containsExactlyInAnyOrder("client", "api")

        // edit
        jiraClient.editIssue(key, updatedTest.toEditPayload(), false)

        // read
        issue = jiraClient.getIssue(key)
        fields = issue["fields"] as Map<String, Any>
        assertThat(fields["summary"]).isEqualTo(updatedTest.name)
        assertThat(fields["description"]).isEqualTo(updatedTest.content)
        assertThat(fields["labels"] as List<String>).containsExactlyInAnyOrder("new")

        // delete
        jiraClient.deleteIssues(listOf(key))
        assertThrows<JiraClientException> { jiraClient.getIssue(key) }
    }

    @Test
    fun `can't get project ID`() {
        assertThrows<JiraClientException> { jiraClient.getProjectId("ABC") }
    }

    @Test
    fun `can't get issue type ID - wrong project ID`() {
        assertThrows<JiraClientException> { jiraClient.getIssueTypeId("Task", "123") }
    }

    @Test
    fun `can't get issue type ID - wrong issue type`() {
        val projectId = jiraClient.getProjectId("COT")
        assertThrows<JiraClientException> { jiraClient.getIssueTypeId("Taskito", projectId) }
    }

    @Test
    fun `can't edit issue`() {
        assertThrows<JiraClientException> { jiraClient.editIssue("ABC-1", mapOf(), false) }
    }

    @Test
    fun `can't delete issue`() {
        assertThrows<JiraClientException> { jiraClient.deleteIssues(listOf("ABC-1")) }
    }

    @Test
    fun `can't create issue`() {
        assertThrows<JiraClientException> { jiraClient.createIssues(listOf(mapOf())) }
    }

    @Test
    fun `can't search for issues`() {
        assertThrows<JiraClientException> { jiraClient.searchIssues("abc") }
    }

}
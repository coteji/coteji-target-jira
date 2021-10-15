package io.github.coteji.tests

import io.github.coteji.model.CotejiTest
import io.github.coteji.targets.JiraTarget
import io.github.coteji.tests.mocks.FakeJiraClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MockedJiraTargetTest {
    private val sourceTestWithoutId = CotejiTest(
        name = "[TEST] Create User",
        content = "Some content",
        attributes = mapOf(Pair("labels", listOf("users", "api")))
    )
    private val sourceTestWithIdOne = CotejiTest(
        id = "COT-1",
        name = "[TEST] Update User",
        content = "Some content updated",
        attributes = mapOf()
    )
    private val sourceTestWithIdTwo = CotejiTest(
        id = "COT-2",
        name = "[TEST] Create Config",
        content = "Some config\nSecond row",
        attributes = mapOf(Pair("labels", listOf("config", "api")))
    )
    private val targetTestWithIdTwo = mapOf(
        Pair("key", "COT-2"),
        Pair(
            "fields", mapOf(
                Pair("summary", "[TEST] Create Config"),
                Pair("description", "Some config old\nSecond row"),
                Pair("labels", listOf("ui"))
            )
        )
    )
    private val targetTestWithIdThree = mapOf(
        Pair("key", "COT-3"),
        Pair(
            "fields", mapOf(
                Pair("summary", "[TEST] Update Config"),
                Pair("description", "Some update config\nSecond row")
            )
        )
    )
    private val jira: JiraTarget
    private val allSourceTests = listOf(sourceTestWithoutId, sourceTestWithIdOne, sourceTestWithIdTwo)

    init {
        jira = JiraTarget(
            baseUrl = "https://coteji.atlassian.net",
            userName = "test@test.com",
            project = "COT",
            testIssueType = "Test Case"
        )
        jira.jiraClient = FakeJiraClient()
    }

    @BeforeEach
    fun setUp() {
        (jira.jiraClient as FakeJiraClient).issues.clear()
        (jira.jiraClient as FakeJiraClient).issues.addAll(listOf(targetTestWithIdTwo, targetTestWithIdThree))
    }

    @Test
    fun `push all force`() {
        jira.pushAll(allSourceTests, true)
        assertThat((jira.jiraClient as FakeJiraClient).issues).containsExactlyInAnyOrder(
            sourceTestWithoutId.copy(id = "COT-101").toMap(),
            sourceTestWithIdOne.copy(id = "COT-102").toMap(),
            sourceTestWithIdTwo.toMap()
        )
    }

    @Test
    fun `push all no force`() {
        jira.pushAll(allSourceTests, false)
        assertThat((jira.jiraClient as FakeJiraClient).issues).containsExactlyInAnyOrder(
            sourceTestWithoutId.copy(id = "COT-101").toMap(),
            sourceTestWithIdOne.copy(id = "COT-102").toMap(),
            targetTestWithIdTwo
        )
    }

    @Test
    fun `push only force`() {
        jira.pushOnly(allSourceTests, true)
        assertThat((jira.jiraClient as FakeJiraClient).issues).containsExactlyInAnyOrder(
            sourceTestWithoutId.copy(id = "COT-101").toMap(),
            sourceTestWithIdOne.copy(id = "COT-102").toMap(),
            sourceTestWithIdTwo.toMap(),
            targetTestWithIdThree
        )
    }

    @Test
    fun `push only no force`() {
        jira.pushOnly(allSourceTests, false)
        assertThat((jira.jiraClient as FakeJiraClient).issues).containsExactlyInAnyOrder(
            sourceTestWithoutId.copy(id = "COT-101").toMap(),
            sourceTestWithIdOne.copy(id = "COT-102").toMap(),
            targetTestWithIdTwo,
            targetTestWithIdThree
        )
    }

    @Test
    fun `dry run no force`() {
        val result = jira.dryRun(allSourceTests, false)
        assertThat(result.testsAdded).hasSize(2)
        assertThat(result.testsUpdated).isEmpty()
        assertThat(result.testsAlreadyUpToDate).hasSize(1)
        assertThat(result.testsDeleted).hasSize(1)
        assertThat(result.testsSyncFailed).isEmpty()
    }

    @Test
    fun `dry run force`() {
        val result = jira.dryRun(allSourceTests, true)
        assertThat(result.testsAdded).hasSize(2)
        assertThat(result.testsUpdated).hasSize(1)
        assertThat(result.testsAlreadyUpToDate).isEmpty()
        assertThat(result.testsDeleted).hasSize(1)
        assertThat(result.testsSyncFailed).isEmpty()
    }

}

private fun CotejiTest.toMap(): Map<String, Any> =
    mapOf(
        Pair("key", this.id!!),
        Pair(
            "fields", mapOf(
                Pair("summary", this.name),
                Pair("description", this.content),
                Pair("labels", if (this.attributes.containsKey("labels")) this.attributes["labels"] else null)
            )
        )
    )

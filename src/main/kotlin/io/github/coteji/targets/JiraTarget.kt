package io.github.coteji.targets

import io.github.coteji.clients.Client
import io.github.coteji.clients.JiraClient
import io.github.coteji.core.TestsTarget
import io.github.coteji.model.CotejiTest
import io.github.coteji.model.PushResult

class JiraTarget(
    private val baseUrl: String,
    private val userName: String,
    private val project: String,
    private val testIssueType: String,
    private val jqlForTests: String = "project = $project and type = \"$testIssueType\"",
    // if true email notification to watchers will be sent
    private val notifyOnUpdates: Boolean = false,
) : TestsTarget {
    var jiraClient: Client = JiraClient(baseUrl, userName)
    private val projectId: String by lazy { jiraClient.getProjectId(project) }
    private val issueTypeId: String by lazy { jiraClient.getIssueTypeId(testIssueType, projectId) }

    override fun dryRun(tests: List<CotejiTest>, force: Boolean): PushResult {
        val remoteTestIds = jiraClient.searchIssues(jqlForTests).map { it["key"] as String }
        val result = PushResult()

        // delete redundant
        val idsInSource = tests.filter { it.id != null }.map { it.id }
        val idsToDelete = remoteTestIds.filter { it !in idsInSource }
        result.testsDeleted.addAll(idsToDelete)

        // add new
        val testsToAdd = tests.filter { it.id == null || it.id !in remoteTestIds }
        result.testsAdded.addAll(testsToAdd)
        result.testsWithNonExistingId.addAll(tests.filter { it.id != null && it.id !in remoteTestIds })

        // update existing
        if (force) {
            tests.filter { it.id in remoteTestIds }.forEach {
                result.testsUpdated.add(it)
            }
        } else {
            tests.filter { it.id in remoteTestIds }.forEach {
                result.testsAlreadyUpToDate.add(it)
            }
        }
        return result
    }

    override fun pushAll(tests: List<CotejiTest>, force: Boolean): PushResult {
        val remoteTestIds = jiraClient.searchIssues(jqlForTests).map { it["key"] as String }
        val result = PushResult()

        // delete redundant
        val idsInSource = tests.filter { it.id != null }.map { it.id }
        val idsToDelete = remoteTestIds.filter { it !in idsInSource }
        if (idsToDelete.isNotEmpty()) {
            jiraClient.deleteIssues(idsToDelete)
            result.testsDeleted.addAll(idsToDelete)
        }

        // add new
        val testsToAdd = tests.filter { it.id == null || it.id !in remoteTestIds }
        if (testsToAdd.isNotEmpty()) {
            val addedIds = jiraClient.createIssues(testsToAdd.map { it.toCreatePayload(projectId, issueTypeId) })
            if (testsToAdd.size != addedIds.size) {
                throw RuntimeException(
                    "Error occurred while adding tests to Jira. " +
                            "Tests to add: ${testsToAdd.size}, added IDs: ${addedIds.size}"
                )
            }
            result.testsAdded.addAll((testsToAdd.indices).map { i -> testsToAdd[i].copy(id = addedIds[i]) })
        }
        result.testsWithNonExistingId.addAll(tests.filter { it.id != null && it.id !in remoteTestIds })

        // update existing
        if (force) {
            tests.filter { it.id in remoteTestIds }.forEach {
                jiraClient.editIssue(it.id!!, it.toEditPayload(), notifyOnUpdates)
                result.testsUpdated.add(it)
            }
        } else {
            tests.filter { it.id in remoteTestIds }.forEach {
                result.testsAlreadyUpToDate.add(it)
            }
        }
        return result
    }

    override fun pushOnly(tests: List<CotejiTest>, force: Boolean): PushResult {
        val remoteTestIds = jiraClient.searchIssues(jqlForTests).map { it["key"] as String }
        val result = PushResult()

        // add new
        val testsToAdd = tests.filter { it.id == null || it.id !in remoteTestIds }
        if (testsToAdd.isNotEmpty()) {
            val addedIds = jiraClient.createIssues(testsToAdd.map { it.toCreatePayload(projectId, issueTypeId) })
            if (testsToAdd.size != addedIds.size) {
                throw RuntimeException(
                    "Error occurred while adding tests to Jira. " +
                            "Tests to add: ${testsToAdd.size}, added IDs: ${addedIds.size}"
                )
            }
            result.testsAdded.addAll((testsToAdd.indices).map { i -> testsToAdd[i].copy(id = addedIds[i]) })
        }
        result.testsWithNonExistingId.addAll(tests.filter { it.id != null && it.id !in remoteTestIds })

        // update existing
        if (force) {
            tests.filter { it.id in remoteTestIds }.forEach {
                jiraClient.editIssue(it.id!!, it.toEditPayload(), notifyOnUpdates)
                result.testsUpdated.add(it)
            }
        } else {
            tests.filter { it.id in remoteTestIds }.forEach {
                result.testsAlreadyUpToDate.add(it)
            }
        }
        return result
    }
}

fun CotejiTest.toCreatePayload(projectId: String, issueTypeId: String): Map<String, Any> =
    mapOf(
        Pair(
            "fields", mapOf(
                Pair("project", mapOf(Pair("id", projectId))),
                Pair("issuetype", mapOf(Pair("id", issueTypeId))),
                Pair("summary", this.name),
                Pair("description", this.content),
                Pair("labels", if (this.attributes.containsKey("labels")) this.attributes["labels"] else null)
            )
        )
    )

fun CotejiTest.toEditPayload(): Map<String, Any> {
    val result = mutableMapOf<String, Any>(
        Pair(
            "fields", mapOf(
                Pair("summary", this.name),
                Pair("description", this.content),
            )
        ),
    )
    if (this.attributes.containsKey("labels")) {
        result["update"] = mapOf(Pair("labels", listOf(mapOf(Pair("set", this.attributes["labels"])))))
    }
    return result
}

package io.github.coteji.tests.mocks

import io.github.coteji.clients.Client

class FakeJiraClient : Client {
    private var newIssuesKeyStart = 101
    val issues = mutableListOf<Map<String, Any>>()

    override fun getIssue(key: String): Map<String, Any> = mapOf()
    override fun searchIssues(jql: String): List<Map<String, Any>> = issues
    override fun deleteIssues(keys: List<String>) {
        issues.removeIf { it["key"] in keys }
    }

    override fun createIssues(payload: List<Map<String, Any>>): List<String> {
        val result = mutableListOf<String>()
        payload.forEach {
            val id = "${newIssuesKeyStart++}"
            val newIssue = mutableMapOf(
                Pair("key", "COT-$id"),
                Pair(
                    "fields", mutableMapOf(
                        Pair("summary", (it["fields"] as Map<*, *>)["summary"]),
                        Pair("description", (it["fields"] as Map<*, *>)["description"]),
                        Pair("labels", (it["fields"] as Map<*, *>)["labels"]),
                    )
                )
            )
            issues.add(newIssue)
            result.add("COT-$id")
        }
        return result
    }

    override fun editIssue(key: String, payload: Map<String, Any>, notifyOnUpdates: Boolean) {
        val issue = issues.firstOrNull { it["key"] == key } ?: throw RuntimeException("No issue with key $key")
        val newIssue = issue.toMutableMap()
        newIssue["fields"] = payload["fields"] as MutableMap<String, Any>
        if (payload.containsKey("update")) {
            (newIssue["fields"] as MutableMap<String, Any>)["labels"] =
                ((payload["update"] as Map<String, Any>)["labels"] as List<Map<String, Any>>)[0]["set"] as List<String>
        }
        issues.removeIf { it["key"] == key }
        issues.add(newIssue)
    }

    override fun getProjectId(project: String): String = "1000"
    override fun getIssueTypeId(issueType: String, projectId: String): String = "1001"
}

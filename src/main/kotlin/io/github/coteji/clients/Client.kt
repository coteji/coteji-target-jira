package io.github.coteji.clients

interface Client {
    fun getIssue(key: String): Map<String, Any>
    fun searchIssues(jql: String): List<Map<String, Any>>
    fun deleteIssues(keys: List<String>)
    fun createIssues(payload: List<Map<String, Any>>): List<String>
    fun editIssue(key: String, payload: Map<String, Any>, notifyOnUpdates: Boolean)
    fun getProjectId(project: String): String
    fun getIssueTypeId(issueType: String, projectId: String): String
}
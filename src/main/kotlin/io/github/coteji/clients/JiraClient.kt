package io.github.coteji.clients

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.HttpException
import com.github.kittinunf.fuel.core.responseUnit
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpDelete
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.httpPut
import com.github.kittinunf.result.Result.Failure
import com.github.kittinunf.result.Result.Success
import com.google.gson.Gson
import mu.KotlinLogging
import java.util.*

class JiraClient(hostUrl: String, userName: String) : Client {
    var issuesChunkSize = 100
    private val logger = KotlinLogging.logger {}
    private val defaultFieldsList = listOf("summary", "description", "labels", "status", "assignee")

    init {
        val encodedCredential =
            Base64.getEncoder().encodeToString("$userName:${System.getenv("COTEJI_JIRA_API_TOKEN")}".toByteArray())
        FuelManager.instance.baseHeaders = mapOf(
            Pair("Authorization", "Basic $encodedCredential"),
            Pair("Accept", "application/json"),
            Pair("Content-Type", "application/json")
        )
        FuelManager.instance.basePath = hostUrl
    }

    override fun getIssue(key: String): Map<String, Any> {
        val (_, _, result) = "/rest/api/latest/issue/$key"
            .httpGet()
            .responseObject<HashMap<String, Any>>()

        when (result) {
            is Failure -> {
                logger.error { String(result.error.errorData) }
                throw JiraClientException("Couldn't get issue with key $key", result.getException())
            }
            is Success -> {
                logger.debug { "Issue $key successfully retrieved" }
                return result.get()
            }
        }
    }

    override fun searchIssues(jql: String): List<Map<String, Any>> {
        val result: MutableList<Map<String, Any>> = mutableListOf()
        val total: Int
        var startAt = 0
        val body = SearchPayload(jql, defaultFieldsList, issuesChunkSize, startAt)
        val (_, _, reqResult) = "/rest/api/latest/search"
            .httpPost()
            .body(Gson().toJson(body))
            .responseObject<HashMap<String, Any>>()

        when (reqResult) {
            is Failure -> {
                logger.error { String(reqResult.error.errorData) }
                throw JiraClientException(reqResult.getException().message, reqResult.getException())
            }
            is Success -> {
                total = (reqResult.get()["total"] as Double).toInt()
                result.addAll(reqResult.get()["issues"] as List<Map<String, Any>>)
            }
        }
        while (startAt + issuesChunkSize < total) {
            startAt += issuesChunkSize
            val (_, _, nextReqResult) = "/rest/api/latest/search"
                .httpPost()
                .body(Gson().toJson(body.copy(startAt = startAt)))
                .responseObject<HashMap<String, Any>>()

            when (nextReqResult) {
                is Failure -> {
                    logger.error { String(nextReqResult.error.errorData) }
                    throw JiraClientException(nextReqResult.getException().message, nextReqResult.getException())
                }
                is Success -> {
                    result.addAll(nextReqResult.get()["issues"] as List<Map<String, Any>>)
                }
            }
        }
        return result
    }

    override fun deleteIssues(keys: List<String>) {
        for (key in keys) {
            val (_, _, result) = "/rest/api/latest/issue/$key"
                .httpDelete(listOf(Pair("deleteSubtasks", "true")))
                .responseUnit()
            when (result) {
                is Failure -> {
                    logger.error { String(result.error.errorData) }
                    throw JiraClientException("Couldn't delete issue with key $key", result.getException())
                }
                is Success -> {
                    logger.debug { "Issue $key was deleted" }
                }
            }
        }
    }

    override fun createIssues(payload: List<Map<String, Any>>): List<String> {
        val body = mapOf(Pair("issueUpdates", payload))
        val (_, _, result) = "/rest/api/latest/issue/bulk"
            .httpPost()
            .body(Gson().toJson(body))
            .responseObject<HashMap<String, Any>>()

        when (result) {
            is Failure -> {
                logger.error { String(result.error.errorData) }
                throw JiraClientException("Couldn't create issues in bulk", result.getException())
            }
            is Success -> {
                val issues = result.get()["issues"] as List<Map<String, Any>>
                val errors = result.get()["errors"] as List<Map<String, Any>>
                errors.forEach { logger.error { it } }
                val keys = issues.map { it["key"] } as List<String>
                logger.debug { "Created issues: $keys" }
                return keys
            }
        }
    }

    override fun editIssue(key: String, payload: Map<String, Any>, notifyOnUpdates: Boolean) {
        val (_, _, result) = "/rest/api/latest/issue/$key"
            .httpPut(listOf(Pair("notifyUsers", notifyOnUpdates)))
            .body(Gson().toJson(payload))
            .responseUnit()
        when (result) {
            is Failure -> {
                logger.error { String(result.error.errorData) }
                throw JiraClientException("Couldn't edit issue with key $key", result.getException())
            }
            is Success -> {
                logger.debug { "Issue $key was updated successfully" }
            }
        }
    }

    override fun getProjectId(project: String): String {
        val (_, _, result) = "/rest/api/latest/project/$project"
            .httpGet()
            .responseObject<HashMap<String, Any>>()

        when (result) {
            is Failure -> {
                logger.error { String(result.error.errorData) }
                throw JiraClientException("Couldn't get project info by key $project", result.getException())
            }
            is Success -> {
                logger.debug { "Project $project info successfully retrieved" }
                return result.get()["id"] as String
            }
        }
    }

    override fun getIssueTypeId(issueType: String, projectId: String): String {
        val (_, _, result) = "/rest/api/latest/issuetype/project"
            .httpGet(listOf(Pair("projectId", projectId)))
            .responseObject<List<HashMap<String, Any>>>()

        when (result) {
            is Failure -> {
                logger.error { String(result.error.errorData) }
                throw JiraClientException("Couldn't get issue types for project ID $projectId", result.getException())
            }
            is Success -> {
                logger.debug { "Issue types for Project ID $projectId successfully retrieved" }
                val issueTypeInfo = result.get().firstOrNull { it["name"] == issueType }
                    ?: throw JiraClientException("Couldn't find issue type '$issueType'", null)
                return issueTypeInfo["id"] as String
            }
        }
    }
}

data class SearchPayload(val jql: String, val fields: List<String>, val maxResults: Int, val startAt: Int)

class JiraClientException(message: String?, cause: Throwable?) : RuntimeException(message, cause)
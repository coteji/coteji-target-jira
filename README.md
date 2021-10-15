![CI](https://github.com/coteji/coteji-target-jira/workflows/CI/badge.svg?branch=master)
[![codecov](https://codecov.io/gh/coteji/coteji-target-jira/branch/master/graph/badge.svg?token=gXXOQPNGfc)](https://codecov.io/gh/coteji/coteji-target-jira)

# Coteji Target: JIRA

This package allows Coteji to push tests to JIRA.

## Usage

Import the package in your configuration file like this:

```kotlin
@file:DependsOn("io.github.coteji:coteji-target-jira:0.1.0")

import io.github.coteji.targets.*
```

## Configuration

Here is the example of configuration:

```kotlin
target = JiraTarget(
            baseUrl = "https://coteji.atlassian.net",
            userName = "maxbarvinskyi@gmail.com",
            project = "COT",
            testIssueType = "Task",
            jqlForTests = "project = COT and type = \"Task\" and summary ~ \"TEST\""
)
```

See the full list of constructor arguments with default values
in [JiraTarget.kt](src/main/kotlin/io/github/coteji/targets/JiraTarget.kt)

API token should be provided via environment variable called `COTEJI_JIRA_API_TOKEN`
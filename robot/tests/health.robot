*** Settings ***
Documentation       Liveness and readiness checks for the prototype services.
...                 URLs default to host-published ports for local runs;
...                 the Jenkins pipeline overrides them with container-network
...                 addresses via --variable.
Library             RequestsLibrary

*** Variables ***
${PY_URL}           http://localhost:8000
${JAVA_URL}         http://localhost:8081
${TIMEOUT}          30s
${RETRY_EVERY}      1s

*** Test Cases ***
Python Service Is Alive
    Endpoint Eventually Returns 200    ${PY_URL}/health/live

Python Service Becomes Ready
    Endpoint Eventually Returns 200    ${PY_URL}/health/ready

Java Service Is Alive
    Endpoint Eventually Returns 200    ${JAVA_URL}/health/live

Java Service Becomes Ready
    Endpoint Eventually Returns 200    ${JAVA_URL}/health/ready

*** Keywords ***
Endpoint Eventually Returns 200
    [Documentation]    Polls until the endpoint answers 200, failing after
    ...                ${TIMEOUT}. Covers both "container not accepting
    ...                connections yet" and "service up but returning 503".
    [Arguments]    ${url}
    Wait Until Keyword Succeeds    ${TIMEOUT}    ${RETRY_EVERY}
    ...    GET    ${url}    expected_status=200

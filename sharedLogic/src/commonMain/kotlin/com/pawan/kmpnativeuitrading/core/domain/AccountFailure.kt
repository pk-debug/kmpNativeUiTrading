/** Safe account failures shared across features; no HTTP status leaks into presentation. */
package com.pawan.kmpnativeuitrading.core.domain

enum class FailureKind { UNAUTHENTICATED, CONFLICT, INVALID_INPUT, RATE_LIMITED, UNAVAILABLE }
open class AccountFailure(val kind: FailureKind, message: String) : Exception(message)

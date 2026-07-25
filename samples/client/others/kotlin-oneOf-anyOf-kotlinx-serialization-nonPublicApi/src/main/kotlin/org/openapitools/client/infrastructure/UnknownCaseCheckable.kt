package org.openapitools.client.infrastructure

/**
 * Implemented by generated types that may hold the synthetic "unknown default" enum case
 * produced when `enumUnknownDefaultCase` is enabled. Allows the oneOf deserializer to reject
 * a variant that only decoded successfully because an unknown enum value was silently
 * mapped to the unknown default case.
 */
internal interface UnknownCaseCheckable {
    val containsUnknownDefaultOpenApiCase: kotlin.Boolean
}

/**
 * Returns true when the receiver — or, for collections, any of its items — implements
 * [UnknownCaseCheckable] and reports that it contains an unknown default enum case.
 * Returns false for null receivers and types that do not implement [UnknownCaseCheckable].
 */
internal fun kotlin.Any?.containsUnknownDefaultOpenApiCase(): kotlin.Boolean = when (this) {
    is UnknownCaseCheckable -> containsUnknownDefaultOpenApiCase
    is kotlin.collections.Iterable<*> -> any { it.containsUnknownDefaultOpenApiCase() }
    is kotlin.Array<*> -> any { it.containsUnknownDefaultOpenApiCase() }
    else -> false
}

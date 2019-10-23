package org.example.static_analyzer.modules

import org.example.static_analyzer.isInt

internal class CheckReturnValues {
    private val returnValues = mutableMapOf<String, Any>()

    operator fun set(name: String, value: Any) {
        returnValues[name] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> get(name: String) : T = returnValues[name] as T

    operator fun contains(name: String) = name in returnValues

    infix fun String.withValue(value: Any) {
        returnValues[this] = value
    }

    operator fun CheckReturnValues.plusAssign(other: CheckReturnValues) {
        val result = this + other
        result.returnValues.forEach { (name, value) -> this[name] = value }
    }

    operator fun plus(other: CheckReturnValues) : CheckReturnValues {
        val thisSummableValues = returnValues.filterValues { it is Number }.mapValues { it.value as Number }
        val otherSummableValues = other.returnValues.filterValues { it is Number }.mapValues { it.value as Number }
        val result = CheckReturnValues()

        thisSummableValues.forEach { (name, value) ->
            var resultValue = value.toDouble()
            resultValue += otherSummableValues[name]?.toDouble() ?: 0.0
            if (resultValue.isInt())
                result[name] = resultValue.toInt()
            else
                result[name] = resultValue
        }

        otherSummableValues.forEach { (name, value) ->
            if (name !in result)
                result[name] = value
        }

        return result
    }

    fun forEach(action: (String, Any) -> Unit) = returnValues.forEach(action)

    fun print(labels: Map<String, String>) {
        labels.forEach { (name, label) ->
            val value = returnValues[name]
            if (value != null)
                println("$label: $value")
        }

        returnValues.filterKeys { it !in labels }.forEach { (name, value) -> println("$name: $value") }
    }
}

internal fun results(initializer: CheckReturnValues.() -> Unit) = CheckReturnValues().apply(initializer)
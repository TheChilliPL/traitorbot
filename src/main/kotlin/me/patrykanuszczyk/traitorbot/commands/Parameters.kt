package me.patrykanuszczyk.traitorbot.commands

import me.patrykanuszczyk.traitorbot.utils.Result
import java.util.*

internal val parseParameterRegex = Regex("""[^"\s]+|"((?:\\"|[^"])+)"""")
fun parseParameters(string: String, vararg parameters: Parameter) : Result<List<String>, String> {
    val split = parseParameterRegex.findAll(string).map {
        (if (it.groupValues[1].isBlank()) it.value else it.groupValues[1])
            .replace("\\\"", "\"")
    }

    var preventDashParameters = false
    val inputQueue = LinkedList<Parameter>()
    val inputsLeft = mutableListOf<String>()
    for (part in split) {
        if (!preventDashParameters && part.startsWith('-')) {
            if (part.length <= 1) return Result.Failure("- is not a valid parameter")
            if (part[1] == '-') {
                if (part.length <= 2) {
                    preventDashParameters = true
                    continue
                }

                // --...
                val parameterName = part.substring(2)
                val parameter = parameters.firstOrNull { it.names.contains(parameterName) }
                    ?: return Result.Failure("Nie znaleziono parametru o nazwie \"$parameterName\"!")
                if (parameter.getInput)
                    inputQueue.add(parameter)
                else
                    parameter.run(null)
                continue
            }
            // -...
            for (parameterChar in part.substring(1)) {
                val parameter = parameters.firstOrNull { it.names.contains(parameterChar.toString()) }
                    ?: return Result.Failure("Nie znaleziono parametru o skrócie \"$parameterChar\"!")
                if (parameter.getInput)
                    inputQueue.add(parameter)
                else
                    parameter.run(null)
            }
            continue
        }

        if (inputQueue.isNotEmpty()) {
            val parameter = inputQueue.poll()
            parameter.run(part)
        } else {
            inputsLeft.add(part)
        }
    }
    if (inputQueue.isNotEmpty()) {
        return Result.Failure(
            "Nie skończyłeś wszystkich parametrów: " +
                inputQueue.map {
                    val name = it.names.first()
                    val dashes = if (name.length == 1) "-" else "--"
                    "`$dashes$name`"
                }.joinToString(", ")
        )
    }

    return Result.Success(inputsLeft)
}
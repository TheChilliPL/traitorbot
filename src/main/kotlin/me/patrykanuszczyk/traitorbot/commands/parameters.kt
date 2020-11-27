package me.patrykanuszczyk.traitorbot.commands

import me.patrykanuszczyk.traitorbot.utils.PeekableIterator
import me.patrykanuszczyk.traitorbot.utils.PeekableIterator.Companion.peekableIterator
import me.patrykanuszczyk.traitorbot.utils.Result
import java.util.*

internal val parseParameterRegex = Regex("""[^"\s]+|"((?:\\"|[^"])+)"""")
fun splitParameters(string: String): Sequence<String> {
    return parseParameterRegex.findAll(string).map {
        (if (it.groupValues[1].isBlank()) it.value else it.groupValues[1])
            .replace("\\\"", "\"")
    }
}

fun parseParameters(string: String, vararg parameters: Parameter) =
    parseParameters(string, null, *parameters)

fun parseParameters(string: String, limit: Int?, vararg parameters: Parameter)
    : Result<List<String>, String> {
    val split = splitParameters(string)

    return parseParameters(split.peekableIterator(), limit, *parameters)
}

fun parseParameters(args: PeekableIterator<String>, limit: Int?, vararg parameters: Parameter)
    : Result<List<String>, String> {
    var preventDashParameters = false
    val inputQueue = LinkedList<Parameter>()
    val inputsLeft = mutableListOf<String>()
    //for (part in args) {
    while (args.hasNext()) {
        val part = args.peek()
        if (!preventDashParameters && part.startsWith('-')) {
            if (part.length <= 1) return Result.Failure("- is not a valid parameter")
            if (part[1] == '-') {
                if (part.length <= 2) {
                    preventDashParameters = true
                    args.next()
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
                args.next()
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
            args.next()
            continue
        }

        if (inputQueue.isNotEmpty()) {
            val parameter = inputQueue.poll()
            parameter.run(part)
        } else {
            if(limit != null && inputsLeft.size >= limit) break
            inputsLeft.add(part)
        }
        args.next()
    }
    if (inputQueue.isNotEmpty()) {
        return Result.Failure(
            "Nie skończyłeś wszystkich parametrów: " +
                inputQueue.joinToString(", ") {
                    val name = it.names.first()
                    val dashes = if (name.length == 1) "-" else "--"
                    "`$dashes$name`"
                }
        )
    }

    return Result.Success(inputsLeft)
}
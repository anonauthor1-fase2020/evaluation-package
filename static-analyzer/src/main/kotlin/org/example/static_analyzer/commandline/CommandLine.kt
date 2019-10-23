package org.example.static_analyzer.commandline

import picocli.CommandLine
import picocli.CommandLine.Unmatched

internal enum class ServiceMetricsSet(val inputName: String) {
    ENGEL("Engel"),
    HIRZALLA("Hirzalla")
}

internal object CommandLine {
    @CommandLine.Spec
    private lateinit var commandSpec: CommandLine.Model.CommandSpec

    @Unmatched
    internal var unmatched = mutableListOf<String>()

    @CommandLine.Option(
        names = ["--csv_file"],
        paramLabel = "CSV_FILEPATH",
        description = ["target csv file"]
    )
    var csvFile: String? = null

    @CommandLine.Option(
        names = ["--additional_intermediate_model"],
        paramLabel = "ADDITIONAL_INTERMEDIATE_MODEL_FILE",
        description = ["an additional intermediate model file to analyze"]
    )
    var additionalIntermediateModels = mutableListOf<String>()

    private var rawServiceMetricsSets = mutableListOf<String>()
        @CommandLine.Option(
            names = ["--service_metrics_set"],
            paramLabel = "[Engel|Hirzalla]",
            description = ["a set of service metrics to employ"]
        )
        set(value) {
            val validMetricsSetNames = ServiceMetricsSet.values().map { it.inputName }
            value.forEach {
                if (it !in validMetricsSetNames)
                    throw CommandLine.ParameterException(commandSpec.commandLine(),"Unknown metrics set $it")
            }

            field = value
        }

    internal fun serviceMetricsSets() = rawServiceMetricsSets.map { rawSetName ->
        ServiceMetricsSet.values().first { it.inputName == rawSetName }
    }

    internal operator fun invoke(args: Array<String>) {
        CommandLine(this)
            .setStopAtUnmatched(false)
            .setStopAtPositional(false)
            .setOverwrittenOptionsAllowed(true)
            .parse(*args)
    }
}
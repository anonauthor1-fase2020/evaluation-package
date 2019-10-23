package org.example.static_analyzer

import de.fhdo.lemma.model_processing.AbstractModelProcessor
import org.example.static_analyzer.commandline.CommandLine

internal class ModelProcessor : AbstractModelProcessor("org.example.static_analyzer") {
    override fun processingFinished(returnCode: Int) {
        if (CommandLine.csvFile != null)
            CsvStore.writeToFile(CommandLine.csvFile!!)
    }
}

fun main(args: Array<String>) {
    CommandLine(args)
    ModelProcessor().run(args)
}
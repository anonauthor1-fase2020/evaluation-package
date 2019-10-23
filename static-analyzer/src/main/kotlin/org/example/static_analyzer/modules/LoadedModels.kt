package org.example.static_analyzer.modules

import de.fhdo.lemma.data.intermediate.IntermediateDataModel
import de.fhdo.lemma.model_processing.asFile
import de.fhdo.lemma.model_processing.command_line.BasicCommandLine
import de.fhdo.lemma.service.intermediate.IntermediateServiceModel
import org.eclipse.emf.ecore.EObject
import org.example.static_analyzer.loadModelRoot
import org.example.static_analyzer.toAbsoluteFilePath
import java.lang.ClassCastException

private val INTERMEDIATE_MODEL_PATH = BasicCommandLine.intermediateModelFile!!.toAbsoluteFilePath()

internal object LoadedModels {
    private val loadedModelRoots = mutableMapOf<String, EObject>()

    operator fun set(filePath: String, modelRoot: EObject) {
        loadedModelRoots[filePath.toAbsoluteFilePath()] = modelRoot
    }

    fun loadModels(filePaths: List<String>) = filePaths.forEach { loadModel(it) }

    fun loadModel(filePath: String) {
        val absoluteFilePath = filePath.toAbsoluteFilePath()
        if (absoluteFilePath in loadedModelRoots)
            return

        val modelRoot = tryLoadModelRoot(absoluteFilePath)
        loadedModelRoots[absoluteFilePath] = modelRoot
    }

    private fun tryLoadModelRoot(filePath: String)
        = try {
            loadModelRoot<IntermediateDataModel>(filePath, INTERMEDIATE_MODEL_PATH)
        } catch (ex: ClassCastException) {
            loadModelRoot<IntermediateServiceModel>(filePath, INTERMEDIATE_MODEL_PATH)
        }

    inline fun <reified T: EObject> forEach(noinline action: (String, T) -> Unit)
        = filterAndMapValues<T>().forEach(action)

    fun isEmpty() = loadedModelRoots.isEmpty()

    inline fun <reified T: EObject> filterAndMapValues()
        = loadedModelRoots.filter { it.value is T }.mapValues { it.value as T }
}
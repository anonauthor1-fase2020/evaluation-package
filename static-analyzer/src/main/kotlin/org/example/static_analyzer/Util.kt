package org.example.static_analyzer

import de.fhdo.lemma.data.intermediate.IntermediateComplexType
import de.fhdo.lemma.data.intermediate.IntermediateDataModel
import de.fhdo.lemma.data.intermediate.IntermediateImportedComplexType
import de.fhdo.lemma.model_processing.asXmiResource as asXmiResourceSimple
import de.fhdo.lemma.utils.LemmaUtils
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import java.io.File
import java.io.FileWriter
import kotlin.math.round
import de.fhdo.lemma.model_processing.asFile
import de.fhdo.lemma.service.intermediate.IntermediateServiceModel
import de.fhdo.lemma.technology.CommunicationType
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.example.static_analyzer.commandline.CommandLine
import org.example.static_analyzer.commandline.ServiceMetricsSet
import java.math.BigDecimal
import java.math.RoundingMode

internal fun Map<*, *>.print(title: String, sumValue: Boolean = false,
    sumLineLabel: String = "") {
    println("$title:")
    var valueSum = 0.0
    entries.forEach {
        (key, value) -> println("\t$key: $value")

        if (sumValue)
            valueSum += (value as? Number)?.toDouble() ?: 0.0
    }

    if (sumValue) {
        kotlin.io.print("\t")
        if (sumLineLabel.isNotEmpty())
            kotlin.io.print("$sumLineLabel: ")

        val printSum = if (valueSum.isInt()) valueSum.toLong() else valueSum
        println(printSum)
    }
}

internal fun Double.isInt() = this == round(this)

internal inline fun <reified T> EObject.countInstances() = filterIsInstance<T>().size

internal inline fun <reified T> EObject.filterIsInstance() : List<T> {
    val results = mutableListOf<T>()
    eAllContents().forEach {
        if (it is T)
            results.add(it)
    }
    return results
}

internal fun sep(c: Char, count: Int = 100) {
    var printString = ""
    repeat(count) { printString += c }
    println(printString)
}

internal fun sep() = sep('-')

internal object CsvStore : HashMap<String, Any>() {
    fun writeToFile(targetFile: String) {
        val csvContent =
            """
                ${keys.joinToString(";")}
                ${values.joinToString(";")}
            """.trimIndent()

        FileWriter(targetFile).use { it.write(csvContent) }
    }
}

internal fun Any.toCsvStore(key: String) {
    CsvStore[key] = this
}

internal fun Number.addToCsvStore(key: String) {
    val targetValue = CsvStore[key] ?: 0
    require(targetValue is Number) { "Existing value for $key is not numeric" }

    val result = toDouble() + targetValue.toDouble()
    if (result.isInt())
        result.toLong().toCsvStore(key)
    else
        result.toCsvStore(key)
}

internal fun IntermediateComplexType.resolve(baseFilePath: String) : IntermediateComplexType {
    /* Determine the defining model and referencing parts (version name, context name, and simple name) of this type */
    val definingModelUri = if (this is IntermediateImportedComplexType) import.importUri else null
    val definingModel = if (definingModelUri != null)
            loadModelRoot(definingModelUri, baseFilePath)
        else
            getDefiningModel<IntermediateDataModel>()
    val (versionName, contextName, simpleName) = getQualifiedNameParts()

    /* Determine sources of this type's referencing parts (version and context instances) */
    val version = if (versionName != null) definingModel.versions.find { it.name == versionName } else null

    val context = if (contextName != null) {
        // A context may be part of a version or reside on the top-level of a data model
        if (version != null)
            version.contexts.find { it.name == contextName }
        else
            definingModel.contexts.find { it.name == contextName }
    } else
        null

    /* Resolve the type */
    val resolvedType = if (version != null && context != null)
    // According to its qualified name, the type is defined in a context within a version
        context.complexTypes.find { it.name == simpleName }
    else if (version != null && context == null)
    // According to its qualified name, the type is directly defined in a version
        version.complexTypes.find { it.name == simpleName }
    else if (context != null)
    // According to its qualified name, the type is directly defined in a context
        context.complexTypes.find { it.name == simpleName }
    else
    // There is neither a version nor a context surrounding the type (according to its qualified name), i.e.,
    // the type is defined on the top-level of the model itself
        definingModel.complexTypes.find { it.name == simpleName }

    require(resolvedType != null) { "Complex type $qualifiedName could not be resolved" }
    return resolvedType
}

internal inline fun <reified T: EObject> EObject.getDefiningModel() : T {
    var container = eContainer()
    while (container !is T)
        container = container.eContainer()
    return container
}

private fun IntermediateComplexType.getQualifiedNameParts() : Triple<String?, String?, String> {
    val qualifiedNameParts = qualifiedName.split(".")

    val version = if (qualifiedNameParts.size == 3) qualifiedNameParts[0] else null

    val context = when(qualifiedNameParts.size) {
        3 -> qualifiedNameParts[1]
        2 -> qualifiedNameParts[0]
        else -> null
    }

    val simpleName = when(qualifiedNameParts.size) {
        3 -> qualifiedNameParts[2]
        2 -> qualifiedNameParts[1]
        else -> qualifiedNameParts[0]
    }

    return Triple(version, context, simpleName)
}

internal fun <T : EObject> loadModelRoot(filePath: String, baseFilePath: String) : T
    = LemmaUtils.convertToAbsoluteFileUri(filePath.removeFileUri(), baseFilePath).asXmiResource().modelRoot()

internal fun String.asXmiResource() : Resource {
    val filePathWithoutUri = this.removeFileUri()
    val file = filePathWithoutUri.asFile()
    require(file.exists()) { "String \"$this\" does not represent a file" }

    val resourceSet = ResourceSetImpl()
    val extensionFactoryMap = Resource.Factory.Registry.INSTANCE.extensionToFactoryMap
    extensionFactoryMap["xmi"] = XMIResourceFactoryImpl()
    val resource = resourceSet.createResource(URI.createFileURI(filePathWithoutUri)) as Resource
    resource.load(null)

    return resource
}

private fun String.removeFileUri() : String = LemmaUtils.removeFileUri(this).removePrefix("//")

@Suppress("UNCHECKED_CAST")
private fun <T : EObject> Resource.modelRoot() : T = contents[0] as T

internal fun String.toAbsoluteFilePath()
    = with(File(this)) {
        if (this.isAbsolute)
            this@toAbsoluteFilePath
        else
            this.absoluteFile.canonicalPath
    }

internal fun Int.divideAndRoundBy(n: Int, roundingScale: Int = 2, divisionByZeroDefault: Double = Double.NaN) : Double {
    if (n == 0)
        return divisionByZeroDefault

    val divisionResult = toDouble() / n.toDouble()
    val rounded = BigDecimal(divisionResult).setScale(roundingScale, RoundingMode.HALF_EVEN)
    return rounded.toDouble()
}

internal fun String.resolveMicroserviceFrom(filePath: String, baseFilePath: String)
    = resolveMicroserviceFrom(loadModelRoot(filePath, baseFilePath))

internal fun String.resolveMicroserviceFrom(serviceModel: IntermediateServiceModel)
    = serviceModel.microservices.first { it.qualifiedName == this }

internal fun EObject.absolutePath() : String {
    return if (eResource().uri.toString().asFile().isAbsolute)
            eResource().uri.toString()
        else
            eResource().uri.toFileString().asFile().canonicalPath
}

internal fun EObject.simpleFileName() = absolutePath().asFile().nameWithoutExtension

@Suppress("UNCHECKED_CAST")
internal val EObject.mainInterface
    get() = this::class.java.interfaces[0] as Class<out EObject>

internal fun metricsSetActivated(set: ServiceMetricsSet) = set in CommandLine.serviceMetricsSets()

internal val CommunicationType.word
    get() = name.toLowerCase()

internal val CommunicationType.title
    get() = word.capitalize()

internal fun Any.isValidCsvValue() = this !is String || "\n" !in this
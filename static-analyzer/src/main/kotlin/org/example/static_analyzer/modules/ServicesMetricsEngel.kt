package org.example.static_analyzer.modules

import de.fhdo.lemma.data.intermediate.*
import de.fhdo.lemma.model_processing.annotations.After
import de.fhdo.lemma.model_processing.annotations.Before
import de.fhdo.lemma.model_processing.annotations.IntermediateModelValidator
import de.fhdo.lemma.model_processing.builtin_phases.intermediate_model_validation.AbstractIntermediateDeclarativeValidator
import de.fhdo.lemma.model_processing.command_line.BasicCommandLine
import de.fhdo.lemma.service.intermediate.*
import de.fhdo.lemma.technology.CommunicationType
import de.fhdo.lemma.technology.ExchangePattern
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.validation.Check
import org.example.static_analyzer.*
import org.example.static_analyzer.commandline.CommandLine
import org.example.static_analyzer.commandline.ServiceMetricsSet
import org.example.static_analyzer.resolve
import org.example.static_analyzer.sep
import org.example.static_analyzer.toAbsoluteFilePath
import org.example.static_analyzer.toCsvStore
import java.util.*
import de.fhdo.lemma.service.intermediate.IntermediatePackage as IntermediateServicePackage

private val overallResults = mutableMapOf<String, CheckReturnValues>()
private var overallServiceCount = -1
private var performMetricsSet = false

@IntermediateModelValidator
internal class ServicesMetricsEngel : AbstractIntermediateDeclarativeValidator() {
    override fun getLanguageNamespace() = IntermediateServicePackage.eNS_URI

    @Before
    internal fun loadModels(resource: Resource) {
        val intermediateModel = resource.contents[0] as? IntermediateServiceModel
        performMetricsSet = intermediateModel != null && metricsSetActivated(ServiceMetricsSet.ENGEL)
        if (!performMetricsSet)
            return

        LoadedModels[BasicCommandLine.intermediateModelFile!!] = intermediateModel!!
        CommandLine.additionalIntermediateModels.forEach { LoadedModels.loadModel(it) }
    }

    @Check
    internal fun M1_InterfaceCountCheck(serviceModel: IntermediateServiceModel) {
        if (!performMetricsSet)
            return

        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("Metric 1 (Engel et al.): Interfaces count for service model $modelFilePath")
            sep()
            sep()
            gatheredResultValues += M1_InterfaceCount(modelRoot)
        }

        val overallServiceCount = getOverallServiceCount()
        gatheredResultValues["metricsEngelOverallServiceCount"] = overallServiceCount
        val overallInterfaceCount = gatheredResultValues.get<Int>("metricsEngelOverallInterfaceCount")
        gatheredResultValues["metricsEngelAverageInterfacesPerServiceCount"] = overallInterfaceCount
            .divideAndRoundBy(overallServiceCount)

        overallResults["Metric 1 (Engel et al.): Interfaces count"] = gatheredResultValues
    }

    private fun getOverallServiceCount() : Int {
        if (overallServiceCount > -1)
            return overallServiceCount

        overallServiceCount = if (LoadedModels.isEmpty())
            0
        else
            LoadedModels.filterAndMapValues<IntermediateServiceModel>()
                .map { (_, serviceModel) -> serviceModel.microservices.size }.sum()

        return overallServiceCount
    }

    private fun M1_InterfaceCount(serviceModel: IntermediateServiceModel) : CheckReturnValues {
        var overallInterfaceCount = 0
        serviceModel.microservices.forEach {
            val interfaceCount = it.interfaces.count()
            interfaceCount.toCsvStore("M1YYYInterfaceCountYYY${it.name}YYY${serviceModel.simpleFileName()}")
            println("\tFor microservice ${it.name}: $interfaceCount")
            overallInterfaceCount += interfaceCount
        }
        sep()

        return results {
            "metricsEngelOverallInterfaceCount" withValue overallInterfaceCount
        }
    }

    @Check
    internal fun M3_DependenciesCountCheck(serviceModel: IntermediateServiceModel) {
        if (!performMetricsSet)
            return

        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("Metric 3 (Engel et al.): Dependencies count for service model $modelFilePath")
            sep()
            sep()
            gatheredResultValues += M3_DependenciesCount(modelFilePath, modelRoot)
        }

        val overallDependencyCount = gatheredResultValues.get<Int>("metricsEngelOverallDependencyCount")
        gatheredResultValues["metricsEngelAverageDependenciesPerServiceCount"] = overallDependencyCount
            .divideAndRoundBy(getOverallServiceCount())

        overallResults["Metric 3 (Engel et al.): Dependencies count"] = gatheredResultValues
    }

    private fun M3_DependenciesCount(modelFilePath: String, serviceModel: IntermediateServiceModel)
        : CheckReturnValues {
        println("Metric 3: Dependencies Count")
        var overallDependenciesCount = 0
        serviceModel.microservices.forEach {
            val dependenciesCount = it.getAllDependencies(modelFilePath).size
            dependenciesCount.toCsvStore("M3YYYDependenciesCountYYY${it.name}YYY${serviceModel.simpleFileName()}")
            println("\tFor microservice ${it.name}: $dependenciesCount")
            overallDependenciesCount += dependenciesCount
        }
        sep()

        return results {
            "metricsEngelOverallDependencyCount" withValue overallDependenciesCount
        }
    }

    private fun IntermediateMicroservice.getAllDependencies(modelFilePath: String) : List<IntermediateMicroservice> {
        val serviceDependencies = mutableListOf<IntermediateMicroservice>()
        requiredMicroservices.forEach {
            if (it.isImported)
                serviceDependencies.add(
                    it.qualifiedName.resolveMicroserviceFrom(it.import.importUri, modelFilePath.toAbsoluteFilePath())
                )
            else {
                val thisModel = it.getDefiningModel<IntermediateServiceModel>()
                serviceDependencies.add(it.qualifiedName.resolveMicroserviceFrom(thisModel))
            }
        }

        requiredInterfaces.forEach {
            val qualifiedServiceName = it.qualifiedName.substringBeforeLast(".")
            if (it.isImported)
                serviceDependencies.add(
                    qualifiedServiceName.resolveMicroserviceFrom(it.import.importUri,
                        modelFilePath.toAbsoluteFilePath())
                )
            else {
                val thisModel = it.getDefiningModel<IntermediateServiceModel>()
                serviceDependencies.add(qualifiedServiceName.resolveMicroserviceFrom(thisModel))
            }
        }

        requiredOperations.forEach {
            val qualifiedInterfaceName = it.qualifiedName.substringBeforeLast(".")
            val qualifiedServiceName = qualifiedInterfaceName.substringBeforeLast(".")
            if (it.isImported)
                serviceDependencies.add(
                    qualifiedServiceName.resolveMicroserviceFrom(it.import.importUri,
                        modelFilePath.toAbsoluteFilePath())
                )
            else {
                val thisModel = it.getDefiningModel<IntermediateServiceModel>()
                serviceDependencies.add(qualifiedServiceName.resolveMicroserviceFrom(thisModel))
            }
        }

        return serviceDependencies
    }

    @Check
    internal fun M5_AverageSynchronousInteractionSize(serviceModel: IntermediateServiceModel)
        = M5_AverageInteractionSizeCheckFor(CommunicationType.SYNCHRONOUS)

    @Check
    internal fun M5_AverageAsynchronousInteractionSize(serviceModel: IntermediateServiceModel)
        = M5_AverageInteractionSizeCheckFor(CommunicationType.ASYNCHRONOUS)

    private fun M5_AverageInteractionSizeCheckFor(communicationType: CommunicationType) {
        if (!performMetricsSet)
            return

        val printableType = communicationType.word
        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("Metric 5 (Engel et al.): Avg. interaction size ($printableType) for service model $modelFilePath")
            sep()
            sep()
            gatheredResultValues += modelRoot.M5_AverageInteractionSize(modelFilePath, communicationType)
        }

        val overallInteractionSize = gatheredResultValues.get<Int>(
            "metricsEngelOverall${communicationType.title}InteractionSize"
        )
        val overallInputInteractionSize = gatheredResultValues.get<Int>(
            "metricsEngelOverall${communicationType.title}InputInteractionSize"
        )
        val overallResultInteractionSize = gatheredResultValues.get<Int>(
            "metricsEngelOverall${communicationType.title}ResultInteractionSize"
        )
        val overallOperationCount = gatheredResultValues.get<Int>(
            "metricsEngelOverall${communicationType.title}OperationCount"
        )
        gatheredResultValues["metricsEngelAverage${communicationType.title}InteractionSize"] =
            overallInteractionSize.divideAndRoundBy(overallOperationCount)
        gatheredResultValues["metricsEngelAverage${communicationType.title}InputInteractionSize"] =
            overallInputInteractionSize.divideAndRoundBy(overallOperationCount)
        gatheredResultValues["metricsEngelAverage${communicationType.title}ResultInteractionSize"] =
            overallResultInteractionSize.divideAndRoundBy(overallOperationCount)

        overallResults["Metric 5 (Engel et al.): Avg. interaction size ($printableType) in bits"] = gatheredResultValues
    }

    private fun IntermediateServiceModel.M5_AverageInteractionSize(modelFilePath: String,
        communicationType: CommunicationType) : CheckReturnValues {
        var overallSize = 0
        var overallInputSize = 0
        var overallResultSize = 0
        var overallOperationsOfCommunicationTypeCount = 0
        var overallParametersOfCommunicationTypeCount = 0
        val absoluteModelFilePath = modelFilePath.toAbsoluteFilePath()
        microservices.forEach { service ->
            println("${communicationType.title} interaction sizes for service ${service.qualifiedName}:")
            var serviceInputSize = 0
            var serviceInputParameterCount = 0
            var serviceResultSize = 0
            var serviceResultParameterCount = 0
            var currentMaxInputSize = 0
            var currentMaxResultSize = 0
            var maxInputOperation: Triple<IntermediateOperation, Int, Int>? = null
            var maxResultOperation: Triple<IntermediateOperation, Int, Int>? = null
            val allOperationsOfCommunicationType = service.interfaces.map { it.operations }.flatten()
                .filter { operation -> operation.parameters.any { it.communicationType == communicationType } }
            allOperationsOfCommunicationType.forEach {
                println("\tOperation ${it.qualifiedName}")
                val (inputSize, inputParameterCount) = it.calcInteractionSizeInBits(absoluteModelFilePath,
                    communicationType)
                println("\t\tInput: $inputSize bits, $inputParameterCount parameters")
                serviceInputSize += inputSize
                serviceInputParameterCount += inputParameterCount

                if (inputSize > currentMaxInputSize) {
                    maxInputOperation = Triple(it, inputSize, inputParameterCount)
                    currentMaxInputSize = inputSize
                }

                val (resultSize, resultParameterCount) = it.calcInteractionSizeInBits(absoluteModelFilePath,
                    communicationType, true)
                println("\t\tResult: $resultSize bits, $resultParameterCount parameters\n")
                serviceResultSize += resultSize
                serviceResultParameterCount += resultParameterCount

                if (resultSize > currentMaxResultSize) {
                    maxResultOperation = Triple(it, resultSize, resultParameterCount)
                    currentMaxResultSize = resultSize
                }
            }

            if (maxInputOperation != null) {
                val maxInputOperationName = maxInputOperation!!.first.qualifiedName
                val (_, size, parameterCount) = maxInputOperation!!

                println("\tOperation with highest input size: $maxInputOperationName, $size bits, " +
                    "$parameterCount parameters\n")
            }

            if (maxResultOperation != null) {
                val maxResultOperationName = maxResultOperation!!.first.qualifiedName
                val (_, size, parameterCount) = maxResultOperation!!

                println("\tOperation with highest result size: $maxResultOperationName, $size bits, " +
                    "$parameterCount parameters\n")
            }

            val overallServiceSize = serviceInputSize + serviceResultSize
            val overallServiceParameterCount = serviceInputParameterCount + serviceResultParameterCount
            println("\tOverall interaction size of service: $overallServiceSize bits, " +
                "$overallServiceParameterCount parameters")
            println("\t\tInput: $serviceInputSize bits, $serviceInputParameterCount parameters")
            println("\t\tResult: $serviceResultSize bits, $serviceResultParameterCount parameters\n")

            val operationCountForCommunicationType = allOperationsOfCommunicationType.size
            val averageServiceSize = overallServiceSize.divideAndRoundBy(operationCountForCommunicationType)
            val averageInputSize = serviceInputSize.divideAndRoundBy(operationCountForCommunicationType)
            val averageResultSize = serviceResultSize.divideAndRoundBy(operationCountForCommunicationType)
            println("\tAverage interaction size of service ($operationCountForCommunicationType operations): " +
                "$averageServiceSize bits")
            println("\t\tInput: $averageInputSize bits")
            println("\t\tResult: $averageResultSize bits\n")

            averageServiceSize.toCsvStore("M5YYYAverageServiceSizeYYY${service.name}YYY${simpleFileName()}")
            averageInputSize.toCsvStore("M5YYYAverageInputSizeYYY${service.name}YYY${simpleFileName()}")
            averageResultSize.toCsvStore("M5YYYAverageResultSizeYYY${service.name}YYY${simpleFileName()}")

            overallSize += overallServiceSize
            overallInputSize += serviceInputSize
            overallResultSize += serviceResultSize
            overallOperationsOfCommunicationTypeCount += operationCountForCommunicationType
            overallParametersOfCommunicationTypeCount += overallServiceParameterCount
        }

        println("Interaction size of all services in model: $overallSize bits, " +
            "$overallOperationsOfCommunicationTypeCount operations, " +
            "$overallParametersOfCommunicationTypeCount parameters")
        sep()

        return results {
            "metricsEngelOverall${communicationType.title}InteractionSize" withValue overallSize
            "metricsEngelOverall${communicationType.title}InputInteractionSize" withValue overallInputSize
            "metricsEngelOverall${communicationType.title}ResultInteractionSize" withValue overallResultSize
            "metricsEngelOverall${communicationType.title}OperationCount" withValue
                overallOperationsOfCommunicationTypeCount
        }
    }

    private fun IntermediateOperation.calcInteractionSizeInBits(modelFilePath: String,
        communicationType: CommunicationType, forResult: Boolean = false) : Pair<Int, Int> {
        val parametersOfCommunicationTypeAndDirection =  parameters.filter {
            it.communicationType == communicationType &&
            (forResult && it.isResultParameter || !forResult && it.isInputParameter)
        }
        val interactionSize = parametersOfCommunicationTypeAndDirection.sumBy {
            it.type.calcSizeInBits(modelFilePath)
        }
        return interactionSize to parametersOfCommunicationTypeAndDirection.count()
    }

    private val IntermediateParameter.isInputParameter
        get() = exchangePattern == ExchangePattern.IN || exchangePattern == ExchangePattern.INOUT

    private val IntermediateParameter.isResultParameter
        get() = exchangePattern == ExchangePattern.OUT || exchangePattern == ExchangePattern.INOUT

    private fun IntermediateType.calcSizeInBits(modelFilePath: String)
        = when(this) {
            is IntermediatePrimitiveType -> calcSizeInBits()
            is IntermediateComplexType -> calcSizeInBits(modelFilePath)
            else -> 0
        }

    private fun IntermediatePrimitiveType.calcSizeInBits() = size ?: 64

    private fun IntermediateComplexType.calcSizeInBits(modelFilePath: String) : Int {
        return when (val resolvedType = this.resolve(modelFilePath)) {
            is IntermediateListType -> resolvedType.calcSizeInBits(modelFilePath)
            is IntermediateDataStructure -> resolvedType.calcSizeInBits(modelFilePath)
            else -> (resolvedType as IntermediateEnumeration).calcSizeInBits()
        }
    }

    private fun IntermediateListType.calcSizeInBits(modelFilePath: String) : Int {
        if (isPrimitiveList)
            return primitiveType.calcSizeInBits()

        var typeSize = 0
        for (field in dataFields) {
            val currentType = field.type
            if (currentType is IntermediateComplexType && currentType == this)
                continue

            typeSize += currentType.calcSizeInBits(modelFilePath)
        }

        return typeSize
    }

    private fun IntermediateDataStructure.calcSizeInBits(modelFilePath: String) : Int {
        var typeSize = 0

        for (field in dataFields) {
            val currentType = field.type
            if (currentType is IntermediateComplexType && currentType == this)
                continue

            typeSize += currentType.calcSizeInBits(modelFilePath)
        }

        return typeSize
    }

    private fun IntermediateEnumeration.calcSizeInBits()
        = fields.sumBy { field ->
            field.initializationValueCompatibleTypes.maxBy { it.calcSizeInBits() }?.calcSizeInBits() ?: 0
        }

    @Check
    internal fun M6_CyclicDependenciesCountCheck(serviceModel: IntermediateServiceModel) {
        if (!performMetricsSet)
            return

        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("Metric 6 (Engel et al.): Cyclic dependencies count $modelFilePath")
            sep()
            sep()
            gatheredResultValues += M6_CyclicDependenciesCount(modelFilePath, modelRoot)
        }

        val overallCyclicDependencyCount = gatheredResultValues.get<Int>("metricsEngelOverallCyclicDependenciesCount")
        gatheredResultValues["metricsEngelAverageCyclicDependenciesPerServiceCount"] = overallCyclicDependencyCount
            .divideAndRoundBy(getOverallServiceCount())

        overallResults["Metric 6 (Engel et al.): Cyclic dependencies count"] = gatheredResultValues
    }

    private fun M6_CyclicDependenciesCount(modelFilePath: String, serviceModel: IntermediateServiceModel)
        : CheckReturnValues {
        var overallCyclesCount = 0
        serviceModel.microservices.forEach { thisService ->
            val visitedChains = mutableSetOf<DependencyChain>()
            val detectedCycles = mutableSetOf<DependencyChain>()
            val dependenciesTodo = ArrayDeque<Pair<IntermediateMicroservice, DependencyChain>>()
            thisService.getAllDependencies(modelFilePath).forEach {
                dependenciesTodo.add(it to DependencyChain(thisService, it))
            }

            while (dependenciesTodo.isNotEmpty()) {
                val (dependency, currentChain) = dependenciesTodo.pop()
                dependency.getAllDependencies(modelFilePath).forEach {
                    if (currentChain.add(it).isCycle())
                        detectedCycles.add(currentChain)
                    else if (currentChain !in visitedChains) {
                        dependenciesTodo.add(it to currentChain)
                        visitedChains.add(currentChain)
                    }
                }
            }

            println("\t${thisService.qualifiedName}:")
            detectedCycles.forEach { println("\t\t$it") }

            val cyclesCount = detectedCycles.size
            cyclesCount.toCsvStore("M6YYYCyclesCountYYY${thisService.name}YYY${serviceModel.simpleFileName()}")
            println("\t\tCount: $cyclesCount")

            overallCyclesCount += cyclesCount
        }
        sep()

        return results {
            "metricsEngelOverallCyclicDependenciesCount" withValue overallCyclesCount
        }
    }

    @After
    fun printOverallResults() {
        if (!performMetricsSet)
            return

        sep('#')
        println("RESULT SUMMARY (Engel et al. metrics)")
        sep('#')
        sep('#')
        overallResults.forEach { (title, values) ->
            println("$title:")
            values.forEach { name, value ->
                if (value.isValidCsvValue())
                    value.toCsvStore(name)
            }
            values.print(mapOf(
                "metricsEngelOverallServiceCount" to "\t# Microservices",
                "metricsEngelOverallInterfaceCount" to "\t# Interfaces",
                "metricsEngelAverageInterfacesPerServiceCount" to "\tAvg. # interfaces per service",
                "metricsEngelOverallDependencyCount" to "\t# Dependencies",
                "metricsEngelAverageDependenciesPerServiceCount" to "\tAvg. # dependencies per service",

                "metricsEngelOverallAsynchronousInteractionSize" to "\tOverall asynchronous interaction size",
                "metricsEngelOverallAsynchronousInputInteractionSize" to
                    "\tOverall asynchronous input interaction size",
                "metricsEngelOverallAsynchronousResultInteractionSize" to
                    "\tOverall asynchronous result interaction size",
                "metricsEngelOverallAsynchronousOperationCount" to "\t# asynchronous operations",
                "metricsEngelAverageAsynchronousInteractionSize" to
                    "\tAvg. asynchronous interaction size per operation",
                "metricsEngelAverageAsynchronousInputInteractionSize" to
                    "\tAvg. asynchronous input interaction size per operation",
                "metricsEngelAverageAsynchronousResultInteractionSize" to
                    "\tAvg. asynchronous result interaction size per operation",

                "metricsEngelOverallSynchronousInteractionSize" to "\tOverall synchronous interaction size",
                "metricsEngelOverallSynchronousInputInteractionSize" to "\tOverall synchronous input interaction size",
                "metricsEngelOverallSynchronousResultInteractionSize" to
                    "\tOverall synchronous result interaction size",
                "metricsEngelOverallSynchronousOperationCount" to "\t# synchronous operations",
                "metricsEngelAverageSynchronousInteractionSize" to "\tAvg. synchronous interaction size per operation",
                "metricsEngelAverageSynchronousInputInteractionSize" to
                    "\tAvg. synchronous input interaction size per operation",
                "metricsEngelAverageSynchronousResultInteractionSize" to
                    "\tAvg. synchronous result interaction size per operation",

                "metricsEngelOverallCyclicDependenciesCount" to "\tOverall cyclic dependencies count",
                "metricsEngelAverageCyclicDependenciesPerServiceCount" to "\tAvg. # cyclic dependencies per service"
            ))
            sep()
        }
        sep('#')
    }
}

private class DependencyChain(vararg initialEntries: IntermediateMicroservice) {
    val entries = mutableListOf<IntermediateMicroservice>()
    private val comparableEntries = mutableListOf<String>()

    init {
        entries.addAll(initialEntries)
        comparableEntries.addAll(initialEntries.map { it.toComparableEntry() })
    }

    fun isCycle() = comparableEntries.first() == comparableEntries.last()

    fun add(dependency: IntermediateMicroservice) : DependencyChain {
        entries.add(dependency)
        comparableEntries.add(dependency.toComparableEntry())
        return this
    }

    private fun IntermediateMicroservice.toComparableEntry()
        = getDefiningModel<IntermediateServiceModel>().absolutePath() + "!" + qualifiedName

    override fun equals(other: Any?): Boolean
        = when {
            this === other -> true
            other == null -> false
            other !is DependencyChain -> false
            else -> comparableEntries.joinToString("") == other.comparableEntries.joinToString("")
        }

    override fun hashCode() = comparableEntries.joinToString("").hashCode()

    override fun toString() = entries.joinToString(" -> ") { it.qualifiedName }
}
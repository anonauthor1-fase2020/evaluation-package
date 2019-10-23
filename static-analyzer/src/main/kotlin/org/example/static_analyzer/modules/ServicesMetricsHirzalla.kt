package org.example.static_analyzer.modules

import de.fhdo.lemma.model_processing.annotations.After
import de.fhdo.lemma.model_processing.annotations.Before
import de.fhdo.lemma.model_processing.annotations.IntermediateModelValidator
import de.fhdo.lemma.model_processing.builtin_phases.intermediate_model_validation.AbstractIntermediateDeclarativeValidator
import de.fhdo.lemma.model_processing.command_line.BasicCommandLine
import de.fhdo.lemma.service.Visibility
import de.fhdo.lemma.service.intermediate.IntermediateMicroservice
import de.fhdo.lemma.service.intermediate.IntermediatePackage
import de.fhdo.lemma.service.intermediate.IntermediateServiceModel
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.validation.Check
import org.example.static_analyzer.*
import org.example.static_analyzer.commandline.CommandLine
import org.example.static_analyzer.commandline.ServiceMetricsSet
import org.example.static_analyzer.divideAndRoundBy
import org.example.static_analyzer.metricsSetActivated
import org.example.static_analyzer.sep
import org.example.static_analyzer.toCsvStore

private val overallResults = mutableMapOf<String, CheckReturnValues>()
private var performMetricsSet = false

@IntermediateModelValidator
internal class ServicesMetricsHirzalla : AbstractIntermediateDeclarativeValidator() {
    override fun getLanguageNamespace() = IntermediatePackage.eNS_URI

    @Before
    internal fun loadModels(resource: Resource) {
        val intermediateModel = resource.contents[0] as? IntermediateServiceModel
        performMetricsSet = intermediateModel != null && metricsSetActivated(ServiceMetricsSet.HIRZALLA)
        if (!performMetricsSet)
            return

        LoadedModels[BasicCommandLine.intermediateModelFile!!] = intermediateModel!!
        CommandLine.additionalIntermediateModels.forEach { LoadedModels.loadModel(it) }
    }

    @Check
    internal fun WSICCheck(serviceModel: IntermediateServiceModel) {
        if (!performMetricsSet)
            return

        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("WSIC Metric (Hirzalla et al.): Weighted Service Interface and Operation Count for service model " +
                modelFilePath)
            sep()
            sep()
            gatheredResultValues += WSIC(modelRoot)
        }

        overallResults["WSIC Metric (Hirzalla et al.): Weighted Service Interface and Operation Count"] =
            gatheredResultValues
    }

    private fun WSIC(serviceModel: IntermediateServiceModel) : CheckReturnValues {
        val serviceModelName = serviceModel.simpleFileName()
        var overallInterfaceCount = 0
        var overallOperationCount = 0
        serviceModel.microservices.forEach { service ->
            val interfaceCount = service.interfaces.filter {
                it.visibility != Visibility.INTERNAL && it.visibility != Visibility.IN_MODEL
            }.size
            interfaceCount.toCsvStore("WSICYYYInterfaceCountYYY${service.name}YYY$serviceModelName")
            println("\t# exposed interfaces for microservice ${service.name}: $interfaceCount")
            overallInterfaceCount += interfaceCount

            val operationCount = service.interfaces.map { it.operations }.flatten().filter {
                it.visibility != Visibility.INTERNAL
            }.size
            operationCount.toCsvStore("WSICYYYOperationCountYYY${service.name}YYY$serviceModelName")
            println("\t# exposed operations for microservice ${service.name}: $operationCount")
            overallOperationCount += operationCount
        }

        println("\n\t# overall exposed interfaces: $overallInterfaceCount")
        overallInterfaceCount.toCsvStore("WSICYYYInterfaceCountYYYOverallYYY$serviceModelName")

        println("\t# overall exposed operations: $overallOperationCount")
        overallOperationCount.toCsvStore("WSICYYYOperationCountYYYOverallYYY$serviceModelName")

        sep()

        return results {
            "metricsHirzallaOverallExposedInterfaceCount" withValue overallInterfaceCount
            "metricsHirzallaOverallExposedOperationCount" withValue overallOperationCount
        }
    }

    @Check
    internal fun SSCheck(serviceModel: IntermediateServiceModel) {
        if (!performMetricsSet)
            return

        val allStatelessServices = mutableListOf<IntermediateMicroservice>()
        val allStatefulServices = mutableListOf<IntermediateMicroservice>()
        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("SS Metric (Hirzalla et al.): Stateless Services for service model $modelFilePath")
            sep()
            sep()
            val returnValues = SS(modelRoot)
            gatheredResultValues += returnValues

            allStatelessServices.addAll(
                returnValues.get<List<IntermediateMicroservice>>("metricsHirzallaStatelessServices")
            )
            allStatefulServices.addAll(
                returnValues.get<List<IntermediateMicroservice>>("metricsHirzallaStatefulServices")
            )
        }

        val overallStatelessServiceCount = gatheredResultValues.get<Int>("metricsHirzallaOverallStatelessServiceCount")
        val overallStatefulServiceCount = gatheredResultValues.get<Int>("metricsHirzallaOverallStatefulServiceCount")
        gatheredResultValues["metricsHirzallaOverallStatelessServices"] = overallStatelessServiceCount
            .divideAndRoundBy(overallStatelessServiceCount + overallStatefulServiceCount)

        gatheredResultValues["metricsHirzallaAllStatelessServiceList"] = allStatelessServices.toPrintableList()
        gatheredResultValues["metricsHirzallaAllStatefulServiceList"] = allStatefulServices.toPrintableList()

        overallResults["SS Metric (Hirzalla et al.): Stateless Services"] = gatheredResultValues
    }

    private fun List<IntermediateMicroservice>.toPrintableList() = toPrintableList { it.qualifiedName }

    private fun <T> List<T>.toPrintableList(getPrintValue: (T) -> String)
            = if (isNotEmpty()) "\n\t\t- " + joinToString("\n\t\t- ") { getPrintValue(it) } else ""

    private fun SS(serviceModel: IntermediateServiceModel) : CheckReturnValues {
        // Stateful methods according to https://tools.ietf.org/html/rfc7231#section-4.3
        val httpStatefulMethods = setOf("post", "put", "delete", "patch")
        val statefulServices = mutableListOf<IntermediateMicroservice>()
        var statelessServices = mutableListOf<IntermediateMicroservice>()
        serviceModel.microservices.forEach { service ->
            val allOperations = service.interfaces.map { it.operations }.flatten()
            val hasStatefulMethod = allOperations.any { operation ->
                operation.aspects
                    .map { it.name.toLowerCase().removeSuffix("mapping") }
                    .any { it in httpStatefulMethods }
            }

            if (hasStatefulMethod)
                statefulServices.add(service)
            else
                statelessServices.add(service)
        }

        val statefulCount = statefulServices.size
        val statelessCount = statelessServices.size
        val metricResult = statefulCount.divideAndRoundBy((statelessCount + statefulCount))
        println("Metric result: $metricResult")
        val serviceModelName = serviceModel.simpleFileName()
        statelessServices.toCsvStore("SSYYYStatelessServicesMetricResultYYY$serviceModelName")
        println("\t# Stateless services: $statefulCount (${statelessServices.joinToString { it.qualifiedName }})")
        statelessCount.toCsvStore("SSYYYStatelessServiceCountYYY$serviceModelName")
        println("\t# Stateful services: $statefulCount (${statefulServices.joinToString { it.qualifiedName }})")
        statefulCount.toCsvStore("SSYYYStatefulServiceCountYYY$serviceModelName")
        sep()

        return results {
            "metricsHirzallaStatelessServices" withValue statelessServices
            "metricsHirzallaOverallStatelessServiceCount" withValue statelessCount
            "metricsHirzallaStatefulServices" withValue statefulServices
            "metricsHirzallaOverallStatefulServiceCount" withValue statefulCount
        }
    }

    @Check
    internal fun NoSCheck(serviceModel: IntermediateServiceModel) {
        if (!performMetricsSet)
            return

        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("NoS Metric (Hirzalla et al.): Number of Services for service model $modelFilePath")
            sep()
            sep()
            gatheredResultValues += NoS(modelRoot)
        }

        overallResults["NoS Metric (Hirzalla et al.): Number of Services"] = gatheredResultValues
    }

    private fun NoS(serviceModel: IntermediateServiceModel) : CheckReturnValues {
        val serviceCount = serviceModel.microservices.size
        println("\tResult: $serviceCount")
        serviceCount.toCsvStore("NoSYYYServiceCountYYY${serviceModel.simpleFileName()}")

        sep()

        return results {
            "metricsHirzallaOverallServiceCount" withValue serviceCount
        }
    }

    @Check
    internal fun SCPCheck(serviceModel: IntermediateServiceModel) {
        if (!performMetricsSet)
            return

        val allMicroservices = LoadedModels.filterAndMapValues<IntermediateServiceModel>()
            .map { (_, serviceModel) -> serviceModel.microservices }.flatten()
        val atomicServices = allMicroservices
            .filter {
                it.requiredMicroservices.isEmpty() &&
                it.requiredInterfaces.isEmpty() &&
                it.requiredOperations.isEmpty()
            }

        val servicesWithRuntimeParameterInitializationFromOtherServices = allMicroservices
            .filter { service ->
                val allServiceOperations = service.interfaces.map { it.operations }.flatten()
                val requiresOtherServicesForParameterInitialization = allServiceOperations.any {
                    operation -> operation.parameters.any {
                        val initializedByOperationOfOtherService =
                            it.initializedByOperation?.localOperation == null ?: false
                        initializedByOperationOfOtherService
                    }
                }
                requiresOtherServicesForParameterInitialization
            }

        val structurallyComposedServices = allMicroservices
            .filter {
                val hasDependenciesToOtherServices =
                    it.requiredMicroservices.isNotEmpty() ||
                    it.requiredInterfaces.isNotEmpty() ||
                    it.requiredOperations.isNotEmpty()

                hasDependenciesToOtherServices && it !in servicesWithRuntimeParameterInitializationFromOtherServices
            }

        // AS
        val atomicServiceCount = atomicServices.size
        // CSwf
        val runtimeInitializationServiceCount = servicesWithRuntimeParameterInitializationFromOtherServices.size  //
        // CSs
        val structurallyComposedServiceCount = structurallyComposedServices.size
        // CS
        val compositeServiceCount = runtimeInitializationServiceCount + structurallyComposedServiceCount
        // SCP = CS / (AS + CSs + CSwf)
        val SCPMetricResult = compositeServiceCount.divideAndRoundBy(
            atomicServiceCount + structurallyComposedServiceCount + runtimeInitializationServiceCount
        )
        // SCPs = CSs / (AS + CSs)
        val SCPsMetricResult = structurallyComposedServiceCount.divideAndRoundBy(
            atomicServiceCount + structurallyComposedServiceCount
        )
        // SCPwf = CSwf / (AS + CSwf)
        val SCPwfMetricResult = runtimeInitializationServiceCount.divideAndRoundBy(
            atomicServiceCount + runtimeInitializationServiceCount
        )

        compositeServiceCount.toCsvStore("metricsHirzallaAllCompositeServiceCount")

        overallResults["SCP Metric (Hirzalla et al.): Service Composition Pattern"] = results {
            "metricsHirzallaAllAtomicServiceList" withValue atomicServices.toPrintableList()
            "metricsHirzallaAllRuntimeInitializationServiceList" withValue
                servicesWithRuntimeParameterInitializationFromOtherServices.toPrintableList()
            "metricsHirzallaAllStructurallyComposedServiceList" withValue structurallyComposedServices.toPrintableList()
            "metricsHirzallaSCPMetricResult" withValue SCPMetricResult
            "metricsHirzallaSCPsMetricResult" withValue SCPsMetricResult
            "metricsHirzallaSCPwfMetricResult" withValue SCPwfMetricResult
        }
    }

    @Check
    internal fun NOVSCheck(serviceModel: IntermediateServiceModel) {
        if (!performMetricsSet)
            return

        val allMicroservices = LoadedModels.filterAndMapValues<IntermediateServiceModel>()
            .map { (_, serviceModel) -> serviceModel.microservices }.flatten()
        val versionsPerService = mutableMapOf<String, MutableList<String>>()
        allMicroservices.forEach {
            val qualifiedName = it.qualifiedName
            if (qualifiedName !in versionsPerService)
                versionsPerService[qualifiedName] = mutableListOf()
            val version = it.version ?: "Original (no explicit version specified)"
            versionsPerService[qualifiedName]!!.add(version)
        }

        val returnValues = CheckReturnValues()
        versionsPerService.forEach { (service, versions) ->
            returnValues["\t$service"] = "# versions: ${versions.size}${versions.toPrintableList { it }}"
        }

        val overallServiceCount = versionsPerService.keys.size
        val overallVersionCount = versionsPerService.values.size
        val NOVSMetricResult = overallVersionCount.divideAndRoundBy(overallServiceCount)
        returnValues["metricsHirzallaNOVSMetricResult"] = NOVSMetricResult

        overallResults["NOVS Metric (Hirzalla et al.): Number of Versions per Service"] = returnValues
    }

    @After
    fun printOverallResults() {
        if (!performMetricsSet)
            return

        sep('#')
        println("RESULT SUMMARY (Hirzalla et al. metrics)")
        sep('#')
        sep('#')
        overallResults.forEach { (title, values) ->
            println("$title:")
            values.forEach { name, value ->
                if (value.isValidCsvValue())
                    value.toCsvStore(name)
            }
            values.print(mapOf(
                "metricsHirzallaOverallExposedInterfaceCount" to "\t# Exposed Interfaces",
                "metricsHirzallaOverallExposedOperationCount" to "\t# Exposed Operations",

                "metricsHirzallaOverallStatelessServices" to "\t Stateless Services metric result",
                "metricsHirzallaOverallStatelessServiceCount" to "\t# Stateless services",
                "metricsHirzallaOverallStatefulServiceCount" to "\t# Stateful services",
                "metricsHirzallaAllStatelessServiceList" to "\tAll stateless services",
                "metricsHirzallaAllStatefulServiceList" to "\tAll stateful services",

                "metricsHirzallaOverallServiceCount" to "\t# Microservices",

                "metricsHirzallaSCPMetricResult" to "\tService Composition Pattern (SCP) metric result",
                "metricsHirzallaSCPsMetricResult" to
                    "\tFraction of composite services constructed using aggregation (SCPs) metric result",
                "metricsHirzallaSCPwfMetricResult" to "\tFraction of composite services composed at runtime " +
                    "leveraging runtime parameter initialization (SCPwf)",
                "metricsHirzallaAllAtomicServiceList" to "\tAll atomic services",
                "metricsHirzallaAllStructurallyComposedServiceList" to "\tAll composite services constructed using " +
                    "aggregation",
                "metricsHirzallaAllRuntimeInitializationServiceList" to "\tAll composite services composed at " +
                    "runtime leveraging runtime parameter initialization",

                "metricsHirzallaNOVSMetricResult" to "\tFraction of overall version and service count (NOVS) metric " +
                    "result"
            ))
            sep()
        }
        sep('#')
    }
}
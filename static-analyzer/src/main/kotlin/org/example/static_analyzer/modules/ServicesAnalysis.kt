package org.example.static_analyzer.modules

import de.fhdo.lemma.model_processing.annotations.After
import de.fhdo.lemma.model_processing.annotations.Before
import de.fhdo.lemma.model_processing.annotations.IntermediateModelValidator
import de.fhdo.lemma.model_processing.asFile
import de.fhdo.lemma.model_processing.builtin_phases.intermediate_model_validation.AbstractIntermediateDeclarativeValidator
import de.fhdo.lemma.model_processing.command_line.BasicCommandLine
import de.fhdo.lemma.service.Visibility
import de.fhdo.lemma.service.intermediate.*
import de.fhdo.lemma.technology.CommunicationType
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.validation.Check
import org.example.static_analyzer.*
import org.example.static_analyzer.addToCsvStore
import org.example.static_analyzer.commandline.CommandLine
import org.example.static_analyzer.sep
import org.example.static_analyzer.simpleFileName
import org.example.static_analyzer.toCsvStore
import java.lang.IllegalArgumentException
import java.util.*
import de.fhdo.lemma.service.intermediate.IntermediatePackage as IntermediateServicePackage

private val overallResults = mutableMapOf<String, CheckReturnValues>()
private var performServicesAnalysis = false

@IntermediateModelValidator
internal class ServicesAnalysis : AbstractIntermediateDeclarativeValidator() {
    override fun getLanguageNamespace() = IntermediateServicePackage.eNS_URI

    @Before
    internal fun loadModels(resource: Resource) {
        val intermediateModel = resource.contents[0] as? IntermediateServiceModel
        performServicesAnalysis = intermediateModel != null
        if (!performServicesAnalysis)
            return

        LoadedModels[BasicCommandLine.intermediateModelFile!!] = intermediateModel!!
        CommandLine.additionalIntermediateModels.forEach { LoadedModels.loadModel(it) }
    }

    @Check
    internal fun countInterfacesAndOperationsCheck(serviceModel: IntermediateServiceModel) {
        var gatheredResultValues = CheckReturnValues()
        var largestInterface: Triple<String, Int, String>? = null
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("Overview of service model $modelFilePath")
            sep()
            sep()
            val resultValues = countInterfacesAndOperations(modelRoot)
            gatheredResultValues += resultValues

            val largestInterfaceName = resultValues.get<String>("largestInterfaceName")
            val largestInterfaceOperationsCount = resultValues.get<Int>("largestInterfaceOperationsCount")
            if (largestInterface == null || largestInterface!!.second < largestInterfaceOperationsCount)
                largestInterface = Triple(largestInterfaceName, largestInterfaceOperationsCount, modelFilePath)
        }

        if (largestInterface != null) {
            gatheredResultValues["largestInterfaceName"] = largestInterface!!.first
            gatheredResultValues["largestInterfaceOperationsCount"] = largestInterface!!.second
            gatheredResultValues["largestInterfaceModelFilePath"] = largestInterface!!.third
        }

        overallResults["Overview of service models"] = gatheredResultValues
    }

    private fun countInterfacesAndOperations(serviceModel: IntermediateServiceModel) : CheckReturnValues {
        val servicesCount = serviceModel.microservices.count()
        println("# Microservices: $servicesCount")
        servicesCount.toCsvStore("allServicesCount")

        val allInterfaces = serviceModel.microservices.map { it.interfaces }.flatten()
        val interfacesCount = allInterfaces.count()
        println("# Interfaces: $interfacesCount")
        interfacesCount.toCsvStore("allInterfacesCount")

        val allOperations = allInterfaces.map { it.operations }.flatten()
        val operationsCount = allOperations.count()
        println("# Operations: $operationsCount")
        operationsCount.toCsvStore("allOperationsCount")
        sep()

        val largestInterface = allInterfaces.maxBy { it.operations.count() }!!
        val largestInterfaceName = largestInterface.qualifiedName
        val largestInterfaceOperationsCount = largestInterface.operations.count() +
            largestInterface.referredOperations.count()
        println("Largest interface: $largestInterfaceName")
        println("# Operations: $largestInterfaceOperationsCount")
        largestInterfaceName.toCsvStore("largestInterface")
        largestInterface.name.toCsvStore("largestInterfaceSimpleName")
        largestInterfaceOperationsCount.toCsvStore("largestInterfaceOperationsCountYYY${serviceModel.simpleFileName()}")
        sep()

        return results {
            "allServicesCount" withValue servicesCount
            "allInterfacesCount" withValue interfacesCount
            "allOperationsCount" withValue operationsCount
            "largestInterfaceName" withValue largestInterfaceName
            "largestInterfaceOperationsCount" withValue largestInterfaceOperationsCount
        }
    }

    @Check
    internal fun gatherProtocols(serviceModel: IntermediateServiceModel) {
        val protocolsInfo = mutableMapOf<String, CommunicationType>()
        serviceModel.microservices.map { it.getEmployedProtocols() }.flatten().forEach {
            val protocolAndDataFormat = "${it.protocol}/${it.dataFormat}"
            protocolsInfo[protocolAndDataFormat] = it.communicationType
        }
        val sortedProtocolsInfo = protocolsInfo.toSortedMap()
        println("Employed protocols:")
        sortedProtocolsInfo.printAndStoreProtocolInfo(CommunicationType.ASYNCHRONOUS)
        sortedProtocolsInfo.printAndStoreProtocolInfo(CommunicationType.SYNCHRONOUS)
        sep()
    }

    private fun SortedMap<String, CommunicationType>.printAndStoreProtocolInfo(communicationType: CommunicationType) {
        val printableCommunicationType = communicationType.name.toLowerCase().capitalize()
        println("\t$printableCommunicationType:")
        val protocolsOfCommunicationType = filter { it.value == communicationType }
        protocolsOfCommunicationType.forEach { (protocol, _) ->
            println("\t\t$protocol")
            val csvProtocolName = protocol.replace("/", "YYY")
            protocol.toCsvStore("usedProtocolYYY$csvProtocolName")
            1.addToCsvStore(csvProtocolName)
        }

        val protocolsCount = protocolsOfCommunicationType.count()
        println("\t# $printableCommunicationType protocols: $protocolsCount")
        1.addToCsvStore("${communicationType.name.toLowerCase()}ProtocolCount")
    }

    private fun IntermediateMicroservice.getEmployedProtocols() : List<IntermediateProtocolSpecification> {
        val resultProtocols = mutableListOf<IntermediateProtocolSpecification>()

        resultProtocols.addAll((this as EObject).getEmployedProtocols())
        resultProtocols.addAll(interfaces.map { it.getEmployedProtocols() }.flatten())
        resultProtocols.addAll(interfaces.map { it.operations }.flatten().map { it.getEmployedProtocols() }.flatten())
        resultProtocols.addAll(
            interfaces.map { it.referredOperations }.flatten().map { it.getEmployedProtocols() }.flatten()
        )

        return resultProtocols
    }

    private fun EObject.getEmployedProtocols() : List<IntermediateProtocolSpecification> {
        val allEndpoints = when (this) {
            is IntermediateMicroservice -> endpoints
            is IntermediateInterface -> endpoints
            is IntermediateOperation -> endpoints
            is IntermediateReferredOperation -> endpoints
            else -> throw IllegalArgumentException("${this::class.java.simpleName} does not have protocols")
        }

        return allEndpoints.map { it.getProtocolSpecification() }
    }

    private fun IntermediateEndpoint.getProtocolSpecification() : IntermediateProtocolSpecification
        = eContainer().getAllProtocols().first {
            it.communicationType == communicationType &&
            it.protocol == protocol &&
            it.dataFormat == dataFormat
        }

    private fun EObject.getAllProtocols() : List<IntermediateProtocolSpecification> {
        return when (this) {
            is IntermediateMicroservice -> protocols
            is IntermediateInterface -> protocols
            is IntermediateOperation -> protocols
            is IntermediateReferredOperation -> protocols
            else -> throw IllegalArgumentException("${this::class.java.simpleName} does not have protocols")
        }
    }

    @Check
    internal fun gatherRestHttpMethodsCheck(serviceModel: IntermediateServiceModel) {
        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateServiceModel> { modelFilePath, modelRoot ->
            println("Overview of REST operations for service model $modelFilePath")
            sep()
            sep()
            gatheredResultValues += gatherRestHttpMethods(modelRoot)
        }

        overallResults["Overview of REST operations"] = gatheredResultValues
    }

    private fun gatherRestHttpMethods(serviceModel: IntermediateServiceModel) : CheckReturnValues {
        val allRestOperations = serviceModel.microservices.asSequence()
            .map { it.interfaces }.flatten()
            .map { it.operations }.flatten()
            .filter { operation -> operation.protocols.any {
                it.communicationType == CommunicationType.SYNCHRONOUS && it.protocol.toLowerCase()== "rest"
            } }
            .toList()

        val (validHttpMethodsCounts, operationsWithoutValidHttpMethods) = allRestOperations.getHttpMethodsDistribution()

        val allRestOperationsCount = allRestOperations.count()
        println("# Operations with REST protocol assigned: $allRestOperationsCount")

        val restOperationsWithExplicitRestEndpoint = allRestOperations.filter { operation ->
            operation.endpoints.any { it.protocol.toLowerCase() == "rest" }
        }
        val restOperationsWithExplicitRestEndpointCount = restOperationsWithExplicitRestEndpoint.count()
        println("# REST operations with explicit endpoint assigned: $restOperationsWithExplicitRestEndpointCount")

        val (explicitEndpointHttpMethod, explicitEndpointInvalidHttpMethod)
            = restOperationsWithExplicitRestEndpoint.getHttpMethodsDistribution()
        println("\nEmployed valid HTTP methods on REST operations with explicit endpoint assigned:")
        explicitEndpointHttpMethod.forEach { (method, count) ->
            println("\t$method: $count")
            count.addToCsvStore("explicitEndpointHttpMethodYYY${method}YYYCount")
        }

        println("\nREST Operations with explicit endpoint assigned WITHOUT valid HTTP method:")
        explicitEndpointInvalidHttpMethod.forEach { (operation, visibility) ->
            println("\t${visibility.name.toLowerCase()} $operation")
        }
        val explicitEndpointInvalidHttpMethodCount = explicitEndpointInvalidHttpMethod.count()
        println("\tCount: $explicitEndpointInvalidHttpMethodCount")
        explicitEndpointInvalidHttpMethodCount.addToCsvStore("explicitEndpointInvalidHttpMethodCount")

        println("\nOperations with REST protocol assigned WITHOUT valid HTTP method:")
        operationsWithoutValidHttpMethods.forEach { (operation, visibility) ->
            println("\t${visibility.name.toLowerCase()} $operation")
        }
        val restOperationsWithoutValidHttpMethodCount = operationsWithoutValidHttpMethods.count()
        println("\tCount: $restOperationsWithoutValidHttpMethodCount")
        restOperationsWithoutValidHttpMethodCount.addToCsvStore("restOperationsWithoutValidHttpMethodCount")

        val validHttpMethodsCountsResults = CheckReturnValues()
        println("\nEmployed valid HTTP methods:")
        validHttpMethodsCounts.forEach { (method, count) ->
            validHttpMethodsCountsResults["\t# $method operations"] = count
            println("\t$method: $count")
            count.addToCsvStore("httpMethodYYY${method}YYYCount")
        }
        sep()

        return validHttpMethodsCountsResults + results {
            "restOperationsWithHttpMethod" withValue validHttpMethodsCounts.values.sum()
            "restOperationsWithExplicitRestEndpointCount" withValue restOperationsWithExplicitRestEndpointCount
        }
    }

    private fun List<IntermediateOperation>.getHttpMethodsDistribution() :
        Pair<Map<String, Int>, Map<String, Visibility>> {
        val httpMethodsCounts = mutableMapOf(
            "get" to 0,
            "head" to 0,
            "post" to 0,
            "put" to 0,
            "delete" to 0,
            "connect" to 0,
            "options" to 0,
            "trace" to 0,
            "patch" to 0
        )

        val operationsWithoutValidHttpMethod = mutableMapOf<String, Visibility>()
        forEach { operation ->
            val httpMethodsOfOperation = operation.aspects
                .map { it.name.toLowerCase().removeSuffix("mapping") }
                .filter { it in httpMethodsCounts.keys }
            if (httpMethodsOfOperation.isNotEmpty())
                httpMethodsOfOperation.forEach { httpMethodsCounts[it] = httpMethodsCounts[it]!! + 1 }
            else
                operationsWithoutValidHttpMethod[operation.qualifiedName] = operation.visibility
        }

        return httpMethodsCounts to operationsWithoutValidHttpMethod
    }

    @After
    fun printOverallResults() {
        if (!performServicesAnalysis)
            return

        sep('#')
        println("RESULT SUMMARY (services' analysis)")
        sep('#')
        sep('#')
        overallResults.forEach { (title, values) ->
            println("$title:")
            values.forEach { name, value ->
                if (value.isValidCsvValue())
                    value.toCsvStore(name)
            }
            values.print(mapOf(
                "allServicesCount" to "\t# Microservices",
                "allInterfacesCount" to "\t# Interfaces",
                "allOperationsCount" to "\t# Operations",
                "largestInterfaceName" to "\tLargest interface",
                "largestInterfaceModelFilePath" to "\t\tModel file",
                "largestInterfaceOperationsCount" to "\t\t# Operations",
                "restOperationsWithHttpMethod" to "\t# REST Operations with HTTP method assigned",
                "restOperationsWithExplicitRestEndpointCount" to "\t# REST operations with explicit endpoint assigned"
            ))
            sep()
        }
        sep('#')
    }
}
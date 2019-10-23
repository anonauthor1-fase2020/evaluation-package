package org.example.static_analyzer.modules

import de.fhdo.lemma.data.intermediate.IntermediateDataModel
import de.fhdo.lemma.data.intermediate.IntermediateDataStructure
import de.fhdo.lemma.data.intermediate.IntermediateEnumeration
import de.fhdo.lemma.data.intermediate.IntermediateListType
import de.fhdo.lemma.model_processing.annotations.After
import de.fhdo.lemma.model_processing.annotations.Before
import de.fhdo.lemma.model_processing.annotations.IntermediateModelValidator
import de.fhdo.lemma.model_processing.builtin_phases.intermediate_model_validation.AbstractIntermediateDeclarativeValidator
import de.fhdo.lemma.model_processing.command_line.BasicCommandLine
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.validation.Check
import org.example.static_analyzer.*
import org.example.static_analyzer.commandline.CommandLine
import org.example.static_analyzer.countInstances
import org.example.static_analyzer.print
import org.example.static_analyzer.sep
import de.fhdo.lemma.data.intermediate.IntermediatePackage as IntermediateDataPackage

private val overallResults = mutableMapOf<String, CheckReturnValues>()
private var performDomainAnalysis = false

@IntermediateModelValidator
internal class DomainAnalysis : AbstractIntermediateDeclarativeValidator() {
    override fun getLanguageNamespace() = IntermediateDataPackage.eNS_URI

    @Before
    internal fun loadModels(resource: Resource) {
        val intermediateModel = resource.contents[0] as? IntermediateDataModel
        performDomainAnalysis = intermediateModel != null
        if (!performDomainAnalysis)
            return

        LoadedModels[BasicCommandLine.intermediateModelFile!!] = intermediateModel!!
        CommandLine.additionalIntermediateModels.forEach { LoadedModels.loadModel(it) }
    }

    @Check
    internal fun countDomainModelCheck(domainModel: IntermediateDataModel) {
        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateDataModel> { modelFilePath, modelRoot ->
            println("Overview for domain model $modelFilePath")
            sep()
            sep()
            gatheredResultValues += countDomainModel(modelRoot)
        }
        overallResults["Overview of domain models"] = gatheredResultValues
    }

    private fun countDomainModel(domainModel: IntermediateDataModel) : CheckReturnValues {
        val boundedContextCount = domainModel.contexts.size
        val structuresPerContext = mutableMapOf<String, Int>()
        val listsPerContext = mutableMapOf<String, Int>()
        val enumsPerContext = mutableMapOf<String, Int>()
        domainModel.contexts.forEach {
            structuresPerContext[it.name] = it.complexTypes.filterIsInstance<IntermediateDataStructure>().size
            listsPerContext[it.name] = it.complexTypes.filterIsInstance<IntermediateListType>().size
            enumsPerContext[it.name] = it.complexTypes.filterIsInstance<IntermediateEnumeration>().size
            1.addToCsvStore("boundedContextYYY${it.name}YYYCount")
        }

        println("# Bounded Contexts: $boundedContextCount")
        sep()

        structuresPerContext.print("# Structures per Bounded Context", true, "\n# Structures in all Bounded Contexts")
        val allStructuresCount = domainModel.countInstances<IntermediateDataStructure>()
        println("# of all structures in domain model $allStructuresCount")
        sep()

        listsPerContext.print("List count per Bounded Context", true, "\n# Lists in all Bounded Contexts")
        val allListsCount = domainModel.countInstances<IntermediateListType>()
        println("# of all lists in domain model $allListsCount")
        sep()

        enumsPerContext.print("Enumeration count per Bounded Context", true, "\n# Enumerations in all Bounded Contexts")
        val allEnumsCount = domainModel.countInstances<IntermediateEnumeration>()
        println("# of all enumerations in domain model $allEnumsCount")
        sep()

        return results {
            "boundedContextCount" withValue boundedContextCount
            "allStructuresCount" withValue allStructuresCount
            "allListsCount" withValue allListsCount
            "allEnumsCount" withValue allEnumsCount
        }
    }

    @Check
    internal fun countDddFeaturesCheck(domainModel: IntermediateDataModel) {
        var gatheredResultValues = CheckReturnValues()
        LoadedModels.forEach<IntermediateDataModel> { modelFilePath, modelRoot ->
            println("DDD feature overview for domain model $modelFilePath")
            sep()
            sep()
            gatheredResultValues += countDddFeatures(modelRoot)
        }
        overallResults["DDD feature overview for domain models"] = gatheredResultValues
    }

    private fun countDddFeatures(domainModel: IntermediateDataModel) : CheckReturnValues {
        val allDataStructures = domainModel.filterIsInstance<IntermediateDataStructure>()
        val featureCounts = mutableMapOf<String, Int>()
        val featureCountsPerBoundedContext = mutableMapOf<String, MutableMap<String, Int>>()
        allDataStructures.forEach { structure -> structure.featureNames.forEach { feature ->
            if (feature !in featureCounts)
                featureCounts[feature] = 1
            else
                featureCounts[feature] = featureCounts[feature]!! + 1

            val context = structure.context?.name
            if (context != null) {
                when {
                    context !in featureCountsPerBoundedContext ->
                        featureCountsPerBoundedContext[context] = mutableMapOf(feature to 1)
                    feature !in featureCountsPerBoundedContext[context]!! ->
                        featureCountsPerBoundedContext[context]!![feature] = 1
                    else ->
                        featureCountsPerBoundedContext[context]!![feature] =
                            featureCountsPerBoundedContext[context]!![feature]!! + 1
                }
            }
        } }

        val resultValues = CheckReturnValues()
        featureCounts.forEach { (feature, count) ->
            println("# of feature $feature: $count")
            count.addToCsvStore("featureYYY${feature}YYYCount")
            resultValues["\t# of feature $feature"] = count
        }
        sep()

        featureCountsPerBoundedContext.forEach { (context, featureCounts) ->
            println("# of features in Bounded Context $context:")
            featureCounts.forEach { (feature, count) ->
                println("\t# of feature $feature: $count")
                count.addToCsvStore("contextYYY${context}YYYfeatureYYY${feature}YYYCount")
            }
        }
        sep()

        return resultValues
    }

    @After
    fun printOverallResults() {
        if (!performDomainAnalysis)
            return

        sep('#')
        println("RESULT SUMMARY (domain analysis)")
        sep('#')
        sep('#')
        overallResults.forEach { (title, values) ->
            println("$title:")
            values.forEach { name, value ->
                if (value.isValidCsvValue())
                    value.toCsvStore(name)
            }
            values.print(mapOf(
                "boundedContextCount" to "\t# Bounded Contexts",
                "allStructuresCount" to "\t# of all structures in all domain models",
                "allListsCount" to "\t# of all lists in all domain models",
                "allEnumsCount" to "\t# of all enumerations in all domain models"
            ))
            sep()
        }
        sep('#')
    }
}
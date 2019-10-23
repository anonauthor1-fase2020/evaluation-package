package org.example.static_analyzer.languages

import de.fhdo.lemma.ServiceDslStandaloneSetup
import de.fhdo.lemma.data.DataDslStandaloneSetup
import de.fhdo.lemma.data.DataPackage
import de.fhdo.lemma.data.intermediate.IntermediatePackage as IntermediateDataPackage
import de.fhdo.lemma.model_processing.annotations.LanguageDescriptionProvider
import de.fhdo.lemma.model_processing.languages.LanguageDescription
import de.fhdo.lemma.model_processing.languages.LanguageDescriptionProviderI
import de.fhdo.lemma.model_processing.languages.XtextLanguageDescription
import de.fhdo.lemma.service.ServicePackage
import de.fhdo.lemma.service.intermediate.IntermediatePackage as IntermediateServicePackage

internal val DATA_DSL_LANGUAGE_DESCRIPTION = XtextLanguageDescription(DataPackage.eINSTANCE, DataDslStandaloneSetup())

internal val INTERMEDIATE_DATA_MODEL_LANGUAGE_DESCRIPTION = LanguageDescription(IntermediateDataPackage.eINSTANCE)

internal val SERVICE_DSL_LANGUAGE_DESCRIPTION = XtextLanguageDescription(ServicePackage.eINSTANCE,
    ServiceDslStandaloneSetup())

internal val INTERMEDIATE_SERVICE_MODEL_LANGUAGE_DESCRIPTION = LanguageDescription(IntermediateServicePackage.eINSTANCE)

@LanguageDescriptionProvider
internal class LanguageDescriptionProvider : LanguageDescriptionProviderI {
    override fun getLanguageDescription(forLanguageNamespace: String) : LanguageDescription? {
        return when(forLanguageNamespace) {
            DataPackage.eNS_URI -> DATA_DSL_LANGUAGE_DESCRIPTION
            IntermediateDataPackage.eNS_URI -> INTERMEDIATE_DATA_MODEL_LANGUAGE_DESCRIPTION
            ServicePackage.eNS_URI -> SERVICE_DSL_LANGUAGE_DESCRIPTION
            IntermediateServicePackage.eNS_URI -> INTERMEDIATE_SERVICE_MODEL_LANGUAGE_DESCRIPTION
            else -> null
        }
    }
}
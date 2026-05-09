package com.fhirtools.plugin.fhir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport
import ca.uhn.fhir.parser.IParser
import ca.uhn.fhir.parser.LenientErrorHandler
import ca.uhn.fhir.validation.FhirValidator
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel

object FhirContextHolder {
    val context: FhirContext by lazy { withPluginClassLoader { FhirContext.forR4() } }

    val resourceTypes: Set<String> by lazy { context.resourceTypes }

    val validator: FhirValidator by lazy { withPluginClassLoader { buildValidator() } }

    // Parses through bad primitive values (unknown coded enums, malformed dates) without
    // throwing. We want the validator to surface every issue inline; a strict parser would
    // bail on the first invalid value and mask everything after it. In HAPI 8.x the default
    // LenientErrorHandler still throws on invalid coded values — disable that explicitly.
    fun parser(): IParser =
        context.newJsonParser()
            .setParserErrorHandler(LenientErrorHandler().setErrorOnInvalidValue(false))

    inline fun <T> withPluginClassLoader(block: () -> T): T {
        val pluginLoader = FhirContextHolder::class.java.classLoader
        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = pluginLoader
        try {
            return block()
        } finally {
            Thread.currentThread().contextClassLoader = previous
        }
    }

    private fun buildValidator(): FhirValidator {
        val support = ValidationSupportChain(
            DefaultProfileValidationSupport(context),
            CommonCodeSystemsTerminologyService(context),
            InMemoryTerminologyServerValidationSupport(context),
        )
        val instance = FhirInstanceValidator(support).apply {
            // dom-6 ("resource should have narrative") and similar best-practice
            // rules fire on freshly-inserted skeletons and most real-world resources.
            // Useful for production review but noisy during dev — suppress so the
            // user sees only real validation issues inline.
            bestPracticeWarningLevel = BestPracticeWarningLevel.Ignore
        }
        return context.newValidator().registerValidatorModule(instance)
    }
}

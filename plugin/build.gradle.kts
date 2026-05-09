import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

configurations.all {
    // xpp3-1.1.6.jar bundles a duplicate javax.xml.namespace.QName which collides
    // with JDK's java.xml module under the IntelliJ PluginClassLoader (LinkageError).
    // HAPI pulls it transitively but R4 JSON validation does not exercise org.xmlpull.*.
    exclude(group = "org.ogce", module = "xpp3")

    // Plugin size diet — drop deps we never exercise on the R4 JSON validation path.
    // We MUST keep org.hl7.fhir.convertors (and all the cross-version FHIR cores it
    // transitively pulls — dstu2, dstu2016may, dstu3, r4b, r5): CommonCodeSystems-
    // TerminologyService initializes VersionCanonicalizer at clinit which references
    // BaseAdvisor_30_50 etc., so removing any of them yields NoClassDefFoundError at
    // validator construction time even if no cross-version conversion actually runs.
    exclude(group = "net.sourceforge.plantuml", module = "plantuml-mit") // 9 MB diagram codegen
    exclude(group = "org.apache.jena") // ~7 MB RDF/SPARQL/SHEX — not used for JSON
    exclude(group = "com.google.protobuf", module = "protobuf-java") // 2 MB FHIR-protobuf
    exclude(group = "org.xerial", module = "sqlite-jdbc") // 14 MB terminology disk cache backend
    exclude(group = "com.ibm.icu", module = "icu4j") // 15 MB Unicode normalization — try without
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    val hapiFhirVersion = "8.8.1"
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-validation:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:$hapiFhirVersion")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.2.6.2")
        bundledPlugin("com.intellij.modules.json")
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    instrumentCode = false
}

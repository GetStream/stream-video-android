/*
 * Copyright (c) 2014-2025 Stream.io Inc. All rights reserved.
 *
 * Licensed under the Stream License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/GetStream/stream-android-base/blob/main/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.getstream.kotlin.base.annotation.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import io.getstream.kotlin.base.annotation.marker.StreamConfiguration
import io.getstream.kotlin.base.annotation.marker.StreamFactory
import io.getstream.kotlin.base.annotation.marker.StreamInternalApi
import io.getstream.kotlin.base.annotation.processor.AnnnotationDateParser.parseDate
import java.io.OutputStreamWriter
import java.time.LocalDate

/**
 * A processor that processes annotations and generates configuration classes.
 *
 * @param environment The environment in which the processor is running.
 */
class ConfigurationAnnotationProcessor(private val environment: SymbolProcessorEnvironment) :
    SymbolProcessor {

    private val logger = environment.logger

    private val streamConfigClassName = StreamConfiguration::class.qualifiedName!!
    private val streamFactoryClsName = StreamFactory::class.qualifiedName!!
    private val streamIntApiClsName = "io.getstream.kotlin.base.annotation.marker.StreamInternalApi"
    private val streamFactoryAnnSpc =
        AnnotationSpec.builder(ClassName.bestGuess(streamFactoryClsName)).build()
    private val streamIntApiAnnSpc =
        AnnotationSpec.builder(ClassName.bestGuess(streamIntApiClsName)).build()

    @Suppress("TooGenericExceptionCaught")
    override fun process(resolver: Resolver): List<KSAnnotated> {
        logger.info("Processing configuration annotations")
        val symbols = resolver.getSymbolsWithAnnotation(streamConfigClassName, true)
        val unprocessed = mutableListOf<KSAnnotated>()
        symbols.forEach { symbol ->
            try {
                logger.info("Found configuration annotation: $symbol")
                val classDeclaration = generateScopeClass(symbol as KSClassDeclaration)
                if (symbol.classKind == ClassKind.CLASS) {
                    processConfigDataClass(symbol, classDeclaration)
                }
            } catch (e: Exception) {
                unprocessed.add(symbol)
                logger.error("Error processing configuration annotation : $e", symbol)
            }
        }
        // We have processed all the annotations
        return unprocessed
    }

    private fun processConfigDataClass(symbol: KSClassDeclaration, classDeclaration: TypeSpec) {
        if (!symbol.modifiers.contains(Modifier.DATA)) {
            logger.warn("Configuration class must be a data class", symbol)
        }
        val configFunction = genConfigFunctionSpec(symbol)
        writeScopeClassToFile(
            classDeclaration,
            configFunction,
            symbol.packageName.asString(),
            environment.codeGenerator
        )
    }

    private fun generateScopeClass(classDeclaration: KSClassDeclaration): TypeSpec {
        val className = classDeclaration.simpleName.asString()
        val scopeClassName = "${className}ConfigScope"

        val classBuilder = TypeSpec.classBuilder(scopeClassName)

        // Generate properties for fields
        classDeclaration.primaryConstructor!!.parameters.forEach { parameter ->
            val propertyName = parameter.name!!.asString()
            val propertyType = parameter.type.resolve().toTypeName().copy(nullable = true)
            val propertySpecBuilder =
                PropertySpec.builder(propertyName, propertyType).mutable(true).initializer("null")
            classBuilder.addProperty(propertySpecBuilder.build())
        }
        val returnType = checkNotNull(classDeclaration.superTypes.firstOrNull()?.toTypeName())
        classBuilder.addAnnotation(streamIntApiAnnSpc).build()
        // Add build function
        val buildFunction =
            FunSpec.builder("build")
                .returns(returnType)
                .addStatement("var obj = ${classDeclaration.toClassName().simpleName}()")
                .also { builder ->
                    classDeclaration.primaryConstructor!!.parameters.forEach { property ->
                        val propertyName = property.name!!.asString()
                        builder.addStatement(
                            "if (this.$propertyName != null) { obj = obj.copy($propertyName = this.$propertyName!!) }"
                        )
                    }
                }
                .addStatement("return obj")
                .build()
        classBuilder.addFunction(buildFunction)
        return classBuilder.build()
    }

    private fun genConfigFunctionSpec(classDeclaration: KSClassDeclaration): FunSpec {
        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val scopeClassName = "${className}ConfigScope"

        val configureLambdaType =
            LambdaTypeName.get(
                receiver = ClassName(packageName, scopeClassName),
                returnType = Unit::class.asTypeName()
            )

        val rawReturnType =
            classDeclaration.superTypes.firstOrNull()?.toTypeName()
                ?: error("No supertype found for $className")
        val returnType = checkNotNull(rawReturnType)

        val functionName =
            returnType.toString().substringAfterLast(".").replaceFirstChar { it.lowercase() }

        val builder =
            FunSpec.builder(functionName)
                .addParameter(
                    ParameterSpec.builder("configure", configureLambdaType)
                        .defaultValue("{}")
                        .build()
                )
                .addAnnotation(streamFactoryAnnSpc)
                .addAnnotation(streamIntApiAnnSpc)

        addDeprecationAnnotations(classDeclaration, builder)

        return builder
            .returns(returnType)
            // Build the body
            .addStatement("val scope = %T()", ClassName(packageName, scopeClassName))
            .addStatement("scope.configure()")
            .addStatement("return scope.build()")
            .build()
    }

    private fun addDeprecationAnnotations(
        classDeclaration: KSClassDeclaration,
        builder: FunSpec.Builder
    ) {
        classDeclaration.annotations.forEach { annotation ->
            val annotationName = annotation.shortName.asString()
            if (annotationName == "StreamDeprecated" || annotationName == "Deprecated") {
                val spec = buildDeprecatedAnnotationSpec(annotation, annotationName)
                builder.addAnnotation(spec)
            }
        }
    }

    private fun buildDeprecatedAnnotationSpec(
        annotation: KSAnnotation,
        annotationName: String
    ): AnnotationSpec {
        val builder =
            AnnotationSpec.builder(Deprecated::class.asTypeName())
                .addMember(
                    "message = %S",
                    "This method is deprecated, choose a different streamConfiguration method that returns a StreamRetryPolicy"
                )

        if (annotationName == "StreamDeprecated") {
            val dateArg =
                annotation.arguments.find { it.name?.asString() == "deprecatedOn" }?.value
                    ?: error("Missing 'deprecatedOn' argument")
            val errorAfterArg =
                annotation.arguments.find { it.name?.asString() == "errorAfterDays" }?.value
                    as? Long ?: error("Missing 'errorAfterDays' argument")

            val parsedDate = parseDate(dateArg.toString())
            val errorAfterDate = parsedDate.plusDays(errorAfterArg)
            val now = LocalDate.now()

            if (now.isAfter(errorAfterDate)) {
                builder.addMember("level = %T.ERROR", DeprecationLevel::class)
            } else {
                builder.addMember("level = %T.WARNING", DeprecationLevel::class)
            }
        }
        return builder.build()
    }

    private fun writeScopeClassToFile(
        scopeClass: TypeSpec,
        configFunction: FunSpec,
        packageName: String,
        codeGenerator: CodeGenerator
    ) {
        val fileSpec = FileSpec.builder(packageName, scopeClass.name!!).addType(scopeClass).build()
        val configFunPackageName = "$packageName.scope."
        val file =
            codeGenerator.createNewFile(
                Dependencies.ALL_FILES,
                configFunPackageName,
                scopeClass.name!!,
                "kt"
            )
        OutputStreamWriter(file).use { writer -> fileSpec.writeTo(writer) }
        val factoryPackageName = "$packageName.factory"
        val fileSpecConfigFunction =
            FileSpec.builder(factoryPackageName, scopeClass.name!!)
                .addFunction(configFunction)
                .build()

        val fileConfigFunction =
            codeGenerator.createNewFile(
                Dependencies.ALL_FILES,
                factoryPackageName,
                scopeClass.name!! + "Factory",
                "kt"
            )
        OutputStreamWriter(fileConfigFunction).use { writer ->
            fileSpecConfigFunction.writeTo(writer)
        }
    }
}

/** A provider for the [ConfigurationAnnotationProcessor]. */
class ConfigurationAnnotationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ConfigurationAnnotationProcessor(environment)
}

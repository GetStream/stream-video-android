package io.getstream.video.codeguard.statein

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.wasm.ir2wasm.getSourceLocation
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName

class StateInIrExtension : IrGenerationExtension {

    @OptIn(FirIncompatiblePluginAPI::class)
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // 🔒 Change only if Call class is renamed or moved
        val callClassFqName =
            FqName("io.getstream.video.android.call.Call")

        val callClass =
            pluginContext.referenceClass(callClassFqName)
                ?: return

        val stateInSymbol =
            pluginContext
                .referenceFunctions(FqName("kotlinx.coroutines.flow.stateIn"))
                .singleOrNull()
                ?: return

        val callRoots = mutableSetOf<IrFunctionSymbol>()

        callClass.owner.declarations.forEach { declaration ->
            when (declaration) {
                is IrFunction -> callRoots += declaration.symbol
                is IrProperty -> {
                    declaration.getter?.symbol?.let(callRoots::add)
                    declaration.setter?.symbol?.let(callRoots::add)
                }
            }
        }

        val callGraph = CallGraph()
        val stateInCalls = mutableListOf<StateInCallSite>()

        moduleFragment.files.forEach { file ->
            file.acceptChildrenVoid(
                CallGraphBuilder(
                    callGraph = callGraph,
                    forbiddenSymbol = stateInSymbol,
                    stateInCalls = stateInCalls,
                    file = file
                )
            )
        }

        val reachable =
            CallGraphTraversal.reachableFromRoots(
                roots = callRoots,
                graph = callGraph
            )

        stateInCalls
            .filter { it.caller in reachable }
            .forEach { violation ->
                val location =
                    violation.call.getSourceLocation(violation.file.fileEntry)

                error(
                    """
                    ❌ Forbidden usage of stateIn() detected in Call execution graph.

                    Call path is rooted at:
                      ${callClassFqName.asString()}

                    Offending call:
                      ${location ?: "Unknown source location"}

                    stateIn() must not be used in Call or any code reachable from it.
                    """.trimIndent()
                )
            }
    }
}
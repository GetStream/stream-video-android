package io.getstream.video.codeguard.statein

import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.expressions.IrCall

internal class CallGraph {
    val edges: MutableMap<IrFunctionSymbol, MutableSet<IrFunctionSymbol>> =
        mutableMapOf()
}

internal object CallGraphTraversal {

    fun reachableFromRoots(
        roots: Set<IrFunctionSymbol>,
        graph: CallGraph
    ): Set<IrFunctionSymbol> {
        val visited = mutableSetOf<IrFunctionSymbol>()
        val stack = ArrayDeque<IrFunctionSymbol>()

        stack.addAll(roots)

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (!visited.add(current)) continue

            graph.edges[current]?.forEach { callee ->
                stack.add(callee)
            }
        }

        return visited
    }
}

internal data class StateInCallSite(
    val caller: IrFunctionSymbol,
    val call: IrCall,
    val file: IrFile
)
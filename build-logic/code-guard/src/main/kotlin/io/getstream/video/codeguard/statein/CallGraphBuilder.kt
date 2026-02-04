package io.getstream.video.codeguard.statein

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

internal class CallGraphBuilder(
    private val callGraph: CallGraph,
    private val forbiddenSymbol: IrFunctionSymbol,
    private val stateInCalls: MutableList<StateInCallSite>,
    private val file: IrFile
) : IrElementVisitorVoid {

    private var currentFunction: IrFunctionSymbol? = null

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitFunction(declaration: IrFunction) {
        val previous = currentFunction
        currentFunction = declaration.symbol
        super.visitFunction(declaration)
        currentFunction = previous
    }

    override fun visitCall(expression: IrCall) {
        val caller = currentFunction ?: return
        val callee = expression.symbol

        callGraph
            .edges
            .getOrPut(caller) { mutableSetOf() }
            .add(callee)

        if (callee == forbiddenSymbol) {
            stateInCalls += StateInCallSite(
                caller = caller,
                call = expression,
                file = file
            )
        }

        super.visitCall(expression)
    }

    override fun visitFunctionReference(expression: IrFunctionReference) {
        val caller = currentFunction ?: return

        callGraph
            .edges
            .getOrPut(caller) { mutableSetOf() }
            .add(expression.symbol)

        super.visitFunctionReference(expression)
    }
}
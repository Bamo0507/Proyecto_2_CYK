package Gramatica.utils

import Gramatica.model.Production
import Gramatica.model.SymbolTable

private fun splitArrow(line: String): Pair<String, String>? {
    val index = line.indexOf("->")
    if (index < 0) return null

    val left = line.substring(0, index).trim()
    val right = line.substring(index + 2).trim()

    if(left.isEmpty() || right.isEmpty()) return null

    return left to right
}

// Funcion para construir producciones
fun buildProductions(
    lines: List<String>
): List<Production> {
    SymbolTable.reset()

    // Recolectar todo lo de la izquierda, como no terminales
    for(rawLine in lines){
        val line = rawLine.trim()

        if(line.isEmpty()) continue

        val parts = splitArrow(line) ?: continue

        val (left, _) = parts // tomar lado izquierdo

        // Si no se ha establecido un no terminal inicial, se establece el primero
        if(SymbolTable.start == null) SymbolTable.start = left

        SymbolTable.nonTerminals += left
    }

    // Recolectar las producciones
    val productionsList = mutableListOf<Production>()

    for(rawLine in lines){
        val line = rawLine.trim()
        if(line.isEmpty()) continue

        val parts = splitArrow(line) ?: continue
        val (left, right) = parts

        val productions = right.split("|")

        for(production in productions){
            val rule = production.trim()
            
            // Epsilon explicito
            if(rule == SymbolTable.EPS){
                productionsList += Production(left, listOf(SymbolTable.EPS))
                continue
            }

            // tokenizar por espacios (mantener minusculas)
            // \\s split por espacios en blanco, tabulaciones, o saltos de linea, uno o mas por el +
            // filter { it.isNotEmpty() } para quitar cadenas vacias
            val tokens = rule.split(Regex("\\s+")).filter { it.isNotEmpty() }

            if(tokens.isEmpty()) continue

            productionsList += Production(left, tokens)
        }
    }

    SymbolTable.rebuildTerminals(productionsList)
    return productionsList
}
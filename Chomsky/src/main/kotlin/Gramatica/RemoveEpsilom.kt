package RemoveEpsilom

import Gramatica.model.Production
import Gramatica.model.SymbolTable

fun removeEpsilom(
    productions: List<Production>
): List<Production> {
    // Identificar anulables
    val anulables = determineNullables(productions)
    println("Símbolos anulables: ${anulables.joinToString(", ")}")

    // Arrancamos con todas las producciones menos las que estan vacias
    var result = productions
        .filterNot { it.right.size == 1 && it.right[0] == SymbolTable.EPS }
        .toMutableList()

    // Generar nuevas producciones
    for (production in productions) {
        val rightSide = production.right
        println("Analizando: ${production.left} -> ${rightSide.joinToString("")}")
    
        // Lista de posiciones que son anulables
        val nullablePositions = rightSide.indices.filter { pos -> rightSide[pos] in anulables }
        val nullableCount = nullablePositions.size

        // Si no tiene anulables, no hace nada
        if (nullableCount == 0) {
            println("No contiene anulables, se mantiene tal cual.")
            continue
        }
    
        // Cantidad de combinaciones posibles 2^n
        val totalCombinations = 1 shl nullableCount
        println("Posiciones anulables: $nullablePositions (total combinaciones a generar: ${totalCombinations - 1})")
    
        for (combination in 1 until totalCombinations) {
            val candidateRightSide = mutableListOf<String>()
    
            for ((position, symbol) in rightSide.withIndex()) {
                // Se obtiene la posicion
                // si no es, da -1
                // si es, da la posicion
                val indexInsideNullable = nullablePositions.indexOf(position)

                // Validar si la pos es de las anulables
                // Si es una combinacion posible, se agrega
                val shouldDrop = indexInsideNullable >= 0 &&
                    ((combination shr indexInsideNullable) and 1) == 1

                if (!shouldDrop) candidateRightSide += symbol
            }
    
            if (candidateRightSide.isNotEmpty()) {
                val newProd = Production(production.left, candidateRightSide)
                if (newProd !in result) {
                    result += newProd
                    println("Agregada: ${newProd.left} -> ${newProd.right.joinToString("")}")
                }
            }
        }
    }

    // Remover producciones repetidas
    result = result.distinct().toMutableList()
    println("Total de producciones tras eliminación de duplicados: ${result.size}")
    println("\nResultado final:")
    result.forEach { println("${it.left} -> ${it.right.joinToString("")}") }
    
    return result
}

fun determineNullables(productions: List<Production>): Set<String> {
    val anulables = mutableSetOf<String>()

    // Flag para llevar control de si hay nuevos anulables
    var changed = true

    while (changed) {
        changed = false
        for (production in productions) {
            val right = production.right

            // Primero los que directamente dan vacio
            val prodVacia = (right.size == 1 && right[0] == SymbolTable.EPS)

            // Anulabilidad indirecta, si alguna produccion tiene todos sus elementos en anulables
            val prodConAnulables = right.isNotEmpty() && right.all { token -> token in anulables }

            // Si es anulable, agregarlo y seguir en ciclo
            if ((prodVacia || prodConAnulables) && anulables.add(production.left)) {
                changed = true
            }
        }
    }
    return anulables
}
package RemoveUnary

import Gramatica.model.Production
import Gramatica.model.SymbolTable
import Gramatica.model.isUnit

fun removeUnaryProductions(
    productions: List<Production>
): List<Production> {
    // Tener producciones agrupados por no terminal
    val productionsByNonTerminal: Map<String, List<Production>> =
        productions.groupBy { it.left }

    // Obtener a no terminales
    val nonTerminals: Set<String> = SymbolTable.nonTerminals.toSet()

    // Grafo de producciones unitarias
    val adyacentUnary: MutableMap<String, MutableSet<String>> =
        nonTerminals.associateWith { mutableSetOf<String>() }.toMutableMap()

    // Generar grafo de unarios    
    for(production in productions){
        if(production.isUnit()){
            val destiny = production.right[0]
            adyacentUnary[production.left]!!.add(destiny)
        }
    }

    // Cierre de unarios
    val closeUnary: MutableMap<String, MutableSet<String>> =
        nonTerminals.associateWith { mutableSetOf(it) }.toMutableMap()

    var changed = true
    while(changed){
        changed = false

        for(nonTerminal in nonTerminals){
            var reachableNonTerminals = closeUnary[nonTerminal]!!

            // Agregar vecinos unitarios
            // si tengo A -> B -> C, debo tener C como reachable de A
            for (neighbor in reachableNonTerminals.toList()){
                for(next in adyacentUnary[neighbor].orEmpty()){
                    if(reachableNonTerminals.add(next)){
                        changed = true
                    }
                }
            }
        }
    }

    // Construccion de producciones finales
    val productionsWithoutUnary = linkedSetOf<Production>()

    for(nonTerminal in nonTerminals){
        for(reachableNonTerminal in closeUnary[nonTerminal].orEmpty()){
            for(nonTerminalProduction in productionsByNonTerminal[reachableNonTerminal].orEmpty()){
                if(!nonTerminalProduction.isUnit()){
                    productionsWithoutUnary.add(Production(nonTerminal, nonTerminalProduction.right))
                }
            }
        }
    }

    return productionsWithoutUnary.toList()
}

package RemoveUseless

import Gramatica.model.Production
import Gramatica.model.SymbolTable

fun removeUselessProductions(
    productions: List<Production>
): List<Production> {
    // Tener producciones agrupados por no terminal
    val productionsByNonTerminal: MutableMap<String, List<Production>> =
        productions.groupBy { it.left }.toMutableMap()

    // Obtener a no terminales
    val nonTerminals = productionsByNonTerminal.keys.toMutableSet()

    // Pt1 - Generadores
    val productive = mutableSetOf<String>()
    var changed = true

    while(changed) {
        changed = false

        for(production in productions){
            // Es productivo si produce no terminal
            val isProductive = production.right.all { symbol ->
                SymbolTable.isTerminal(symbol) || symbol in productive // Para manejar no terminales generadores
            }

            if(isProductive && productive.add(production.left)){
                changed = true
            }
        }
    }

    // Identificar no generadores
    val uselessNonTerminals = nonTerminals.filter { it !in productive }.toMutableSet()

    // Quitar no generadores, y producciones que los tengan
    for(nonTerminal in productionsByNonTerminal.keys.toList()){
        if(nonTerminal in uselessNonTerminals){
            productionsByNonTerminal.remove(nonTerminal)
        } else {
            val newProductions = productionsByNonTerminal[nonTerminal]!!.filter { production ->
                production.right.all { symbol ->
                    SymbolTable.isTerminal(symbol) || symbol in productive
                }
            }
            
            if(newProductions.isEmpty()){
                productionsByNonTerminal.remove(nonTerminal)
                uselessNonTerminals.add(nonTerminal)
            } else {
                productionsByNonTerminal[nonTerminal] = newProductions
            }
        }
    }

    // Pt2 - Quitar inalcanzables
    val newNonTerminals = productionsByNonTerminal.keys.toMutableSet()

    val reachableNonTerminals = mutableSetOf<String>()
    val stack = ArrayDeque<String>()

    val start = SymbolTable.start
    if (start != null && start in newNonTerminals) stack.add(start)

    while(stack.isNotEmpty()){
        val nonTerminal = stack.removeFirst()

        if(reachableNonTerminals.add(nonTerminal)){
            val prods = productionsByNonTerminal[nonTerminal].orEmpty()
            for(prod in prods){
                for(symbol in prod.right){
                    if(SymbolTable.isNonTerminal(symbol) && symbol !in reachableNonTerminals){
                        stack.add(symbol)
                    }
                }
            }
        }
    }

    // Quitar no alcanzables
    for(nonTerminal in productionsByNonTerminal.keys.toList()){
        if(nonTerminal !in reachableNonTerminals){
            productionsByNonTerminal.remove(nonTerminal)
        } else {
            val newProductions = productionsByNonTerminal[nonTerminal]!!.filter { prod ->
                prod.right.all { ch -> 
                    !SymbolTable.isNonTerminal(ch) || ch in reachableNonTerminals
                }
            }
            
            if(newProductions.isEmpty()){
                productionsByNonTerminal.remove(nonTerminal)
            } else {
                productionsByNonTerminal[nonTerminal] = newProductions
            }
            
        }
    }

    return productionsByNonTerminal.values.flatten().toList()
}
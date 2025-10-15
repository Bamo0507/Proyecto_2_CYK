package TurnToChomsky

import Gramatica.model.Production
import Gramatica.model.SymbolTable

// Funciones para validar forma cnf
// Validar si es NO_Terminal -> Terminal
fun isCNFUnitTerminal(prod: Production): Boolean =
    prod.right.size == 1 && SymbolTable.isTerminal(prod.right[0])

// Validar si es NO_TERMINAL -> DOS_NO_TERMINALES
fun isCNFBinaryNonterminals(prod: Production): Boolean =
    prod.right.size == 2 && prod.right.all { SymbolTable.isNonTerminal(it) }

// Union de Validacion de dos anteriores
fun isCNF(prod: Production): Boolean = 
    isCNFUnitTerminal(prod) || isCNFBinaryNonterminals(prod)
// -========================================================

// Funcion para generar nuevos no terminales
private fun nextNonTerminal(
    basePrefix: String,
    counter: Int
): String = basePrefix + counter.toString()

// Funcion principal
fun turnToChomsky(
    productions: List<Production>
): List<Production> {
    // Helpers para generar nuevos no terminales
    val basePrefix = "X"
    var counter = 0

    // Acumulando producciones
    // set para evitar crear la misma produccion 2 veces
    val result = linkedSetOf<Production>()
    result += productions

    // Mapas para ayuda
    val terminalToNonTerminal = mutableMapOf<String, String>()
    val pairToNonTerminal = mutableMapOf<Pair<String, String>, String>()

    // Garantizar que un NoTerminal que hagamos no exista ya
    fun freshNonTerminal(): String {
        var candidate: String

        while(true) {
            candidate = nextNonTerminal(basePrefix, counter++)
            if(candidate !in SymbolTable.nonTerminals && candidate !in SymbolTable.terminals){
                break
            }
        }
        // Registrar nuevo no terminal en neustro global object
        SymbolTable.nonTerminals += candidate
        return candidate
    }

    // Generar forma de Chomsky
    var changed = true
    while(changed){
        changed = false

        val snapshot = result.toList()

        for(production in snapshot){
            if(isCNF(production)) continue

            // Copia de lado derecho
            val rightSide = production.right.toMutableList()
            var modified = false

            // Reemplazar terminales en RHS de longitud >= 2
            if(rightSide.size >= 2){
                for(index in rightSide.indices){
                    val symbol = rightSide[index]
                    if(SymbolTable.isTerminal(symbol)){
                        val nonTerminal = terminalToNonTerminal.getOrPut(symbol){
                            val fresh = freshNonTerminal()
                            result.add(Production(fresh, listOf(symbol)))

                            fresh
                        }

                        // Reemplazar terminal por no terminal
                        if(rightSide[index] != nonTerminal){
                            rightSide[index] = nonTerminal
                            modified = true
                        }
                    }
                }   
            }

            // Reemplazar si la produccion es > 2 (ya ahorita tendrian que ser solo no terminales)
            if(rightSide.size > 2){
                while(rightSide.size > 2){
                    val non_terminal1 = rightSide[rightSide.size - 2]
                    val non_terminal2 = rightSide[rightSide.size - 1]

                    val key = non_terminal1 to non_terminal2

                    val nonTerminalPair = pairToNonTerminal.getOrPut(key){
                        val fresh = freshNonTerminal()
                        result.add(Production(fresh, listOf(non_terminal1, non_terminal2)))

                        fresh
                    }

                    // Reemplazar par de no terminales por nuevo no terminal
                    rightSide.removeAt(rightSide.lastIndex)
                    rightSide.removeAt(rightSide.lastIndex)
                    rightSide.add(nonTerminalPair)

                    modified = true
                }
            }

            if(modified){
                // Reemplazar produccion original por transformada
                result.remove(production)
                result.add(Production(production.left, rightSide))
                changed = true
            }
        }
    }
    // Actualizar terminales globales
    SymbolTable.rebuildTerminals(result.toList())

    //Validar que todo esté en cnf
    assert(result.all(::isCNF)) {
        "No se logró convertir todas las producciones a forma de Chomsky"
    }

    return result.toList()
}
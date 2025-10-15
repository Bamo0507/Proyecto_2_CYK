package Gramatica.model

// Este objeto servirá para llevar track
// de los que seriran como no terminales
// y terminales
object SymbolTable {
    val nonTerminals = linkedSetOf<String>()
    val terminals = linkedSetOf<String>()

    // No Terminal Inicial
    var start: String? = null 

    // Simbolo Epsilon
    const val EPS = "ε"

    fun reset() {
        nonTerminals.clear()
        terminals.clear()
        start = null
    }

    fun isNonTerminal(tok: String): Boolean = tok in nonTerminals
    fun isTerminal(tok: String): Boolean = tok != EPS && tok in terminals

    // Construcción de terminales a partir de las producciones
    fun rebuildTerminals(allProductions: List<Production>) {
        terminals.clear()
        for (p in allProductions) {
            for (tok in p.right) {
                if (tok != EPS && tok !in nonTerminals) {
                    terminals += tok
                }
            }
        }
    }
}
package Gramatica.model

// Clases----------------------------------------
data class Production(
    val left: String,
    val right: List<String>
)

fun Production.isUnit(): Boolean =
    right.size == 1 && SymbolTable.isNonTerminal(right[0])
    
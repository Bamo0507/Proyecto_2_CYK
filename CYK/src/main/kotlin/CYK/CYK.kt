package CYK

import Gramatica.model.Production
import Gramatica.model.SymbolTable
import TurnToChomsky.isCNFUnitTerminal

sealed class ParseTree {
    data class Leaf(
        val nonTerminal: String,
        val token: String
    ) : ParseTree()

    data class Node(
        val nonTerminal: String,
        val left: ParseTree,
        val right: ParseTree
    ) : ParseTree()
}

data class CYKResult(
    val accepted: Boolean,
    val trees: List<ParseTree>
)

// Backpointers
private sealed class Backpointer {
     // Solo se tiene A -> a
    data object Terminal : Backpointer()
    
    // Se tiene A -> BC
    data class Binary(
        val split: Int,
        val leftNonTerminal: String,
        val rightNonTerminal: String,
    ) : Backpointer()
}

// Funcion para sanitizar
// y obtener la lista de palabras en oracion
private fun sanitizeAndTokenize(sentence: String): List<String> =
    sentence
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{Nd}\\s]+"), "") // quita signos como comas/puntos, conserva letras y dígitos
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }

// Indexacion de gramatica
private data class GrammarIndex(
    // Mapear un terminal a los no terminales que los producen
    val terminalMap: Map<String, Set<String>>,

    // Mapear un par de no terminales a los no terminales que los producen
    val pairMap: Map<Pair<String, String>, Set<String>>,
)

// Construir Index
private fun buildGrammarIndex(
    productions: List<Production>
): GrammarIndex {
    val terminalMap = mutableMapOf<String, MutableSet<String>>()
    val pairMap = mutableMapOf<Pair<String, String>, MutableSet<String>>()

    for(production in productions){
        if(isCNFUnitTerminal(production)){
            val nonTerminal = production.left
            val terminal = production.right[0]

            terminalMap.getOrPut(terminal){ mutableSetOf() }.add(nonTerminal)
        } else {
            val nonTerminalLeft = production.left
            val nonTerminalRight1 = production.right[0]
            val nonTerminalRight2 = production.right[1]

            pairMap.getOrPut(nonTerminalRight1 to nonTerminalRight2){ mutableSetOf() }.add(nonTerminalLeft)
        }
    }

    return GrammarIndex(
        terminalMap = terminalMap.mapValues { it.value.toSet() },
        pairMap = pairMap.mapValues { it.value.toSet() }
    )
}

// Construcción de árboles desde la tabla
private fun buildTreesFromCell(
    table: Array<Array<MutableMap<String, MutableList<Backpointer>>>>,
    tokens: List<String>, // hojas
    start: Int,
    len: Int, // longitud de la oracion
    nonTerminal: String, // no terminal que se esta buscando
    out: MutableList<ParseTree>
){
    val cell = table[start][len]
    val backpointers = cell[nonTerminal] ?: return

    for(backpointer in backpointers){
        when(backpointer){
            is Backpointer.Terminal -> {
                if(len == 1) {
                    out += ParseTree.Leaf(nonTerminal, tokens[start])
                }
            }
            is Backpointer.Binary -> {
                val leftTrees = mutableListOf<ParseTree>()
                val rightTrees = mutableListOf<ParseTree>()

                buildTreesFromCell(table, tokens, start, backpointer.split, backpointer.leftNonTerminal, leftTrees)
                buildTreesFromCell(table, tokens, start + backpointer.split, len - backpointer.split, backpointer.rightNonTerminal, rightTrees)

                for(leftTree in leftTrees){
                    for(rightTree in rightTrees){
                        out += ParseTree.Node(nonTerminal, leftTree, rightTree)
                    }
                }
            }
        }
    }
}

// Algoritmo CYK
fun cyk(
    productionsCNF: List<Production>,
    sentence: String
): CYKResult {
    val startNT = SymbolTable.start ?: error("SymbolTable.start no está definido.")

    // Tokenizar oracion
    val tokens = sanitizeAndTokenize(sentence)

    if(tokens.isEmpty()){
        return CYKResult(accepted = false, trees = emptyList())
    }

    // Construir indices de gramatica
    val index = buildGrammarIndex(productionsCNF)

    val tokenSize = tokens.size

    val table = Array(tokenSize) {
        Array(tokenSize +1) {
            mutableMapOf<String, MutableList<Backpointer>>()
        }
    }

    // comenzar con longitud 1 en palabras
    for(i in 0 until tokenSize){
        val token = tokens[i]
        val nonTerminals = index.terminalMap[token].orEmpty()

        for(nonTerminal in nonTerminals){
            table[i][1].getOrPut(nonTerminal) { mutableListOf() }.add(Backpointer.Terminal)
        }
    }

    // llenado de longitud de combs 2...n
    for(len in 2..tokenSize){
        for(start in 0..(tokenSize - len)){
            for(split in 1 until len){
                val leftCell = table[start][split]
                val rightCell = table[start + split][len - split]

                if(leftCell.isEmpty() || rightCell.isEmpty()) continue

                for(leftNonTerminal in leftCell.keys){
                    for(rightNonTerminal in rightCell.keys){
                        val parents = index.pairMap[leftNonTerminal to rightNonTerminal].orEmpty()
                        if(parents.isEmpty()) continue

                        for(nonTerminal in parents){
                            table[start][len].getOrPut(nonTerminal) { mutableListOf() }
                                .add(Backpointer.Binary(split, leftNonTerminal, rightNonTerminal))
                        }
                    }
                }
            }
        }
    }

    val accepts = table[0][tokenSize].containsKey(startNT)

    if(!accepts) return CYKResult(accepted = false, trees = emptyList())

    val trees = mutableListOf<ParseTree>()
    buildTreesFromCell(table, tokens, start = 0, len = tokenSize, nonTerminal = startNT, out = trees)

    return CYKResult(true, trees)
}
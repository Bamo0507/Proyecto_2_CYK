import java.io.File

import Gramatica.utils.buildProductions
import RemoveEpsilom.removeEpsilom
import RemoveUnary.removeUnaryProductions
import RemoveUseless.removeUselessProductions
import TurnToChomsky.turnToChomsky
import CYK.cyk
import CYK.ParseTree

// Pretty printer sencillo para el árbol de parseo
private fun printParseTree(tree: ParseTree, indent: String = "") {
    when (tree) {
        is ParseTree.Leaf -> println("$indent${tree.nonTerminal} -> '${tree.token}'")
        is ParseTree.Node -> {
            println("$indent${tree.nonTerminal}")
            printParseTree(tree.left, indent + "  ")
            printParseTree(tree.right, indent + "  ")
        }
    }
}

fun main() {
    val grammarPath = "src/main/kotlin/Texts/gramatica.txt"
    val testSentence = "She cooks with a dog"

    println("Leyendo gramática de: $grammarPath")
    val file = File(grammarPath)
    if (!file.exists()) {
        println("ERROR: No se encontró el archivo $grammarPath")
        return
    }
    val lines = file.readLines()

    // Conversion CNF
    println("Construyendo producciones (parser en dos pasadas)")
    val productions = buildProductions(lines)
    println("Total producciones parseadas: ${productions.size}")

    println("\nEliminar producciones epsilom")
    val noEps = removeEpsilom(productions)
    println("Total producciones sin ε: ${noEps.size}")

    println("\nEliminar producciones unitarias")
    val noUnary = removeUnaryProductions(noEps)
    println("Total producciones sin unitarias: ${noUnary.size}")

    println("\nEliminar producciones inútiles")
    val useful = removeUselessProductions(noUnary)
    println("Total producciones útiles: ${useful.size}")

    println("\nConvertir a CNF")
    val cnf = turnToChomsky(useful)
    println("Total producciones en CNF: ${cnf.size}")

    // CYK
    println("\nCYK")
    println("Oración de prueba: \"$testSentence\"")

    val t0 = System.nanoTime()
    val cykResult = cyk(cnf, testSentence)
    val t1 = System.nanoTime()
    val elapsedMs = (t1 - t0) / 1_000_000.0
    println("Tiempo de evaluación CYK: %.3f ms".format(elapsedMs))

    if (cykResult.accepted) {
        println("La oración SÍ es generada por la gramática.")
        val totalTrees = cykResult.trees.size
        println("Se generaron $totalTrees árbol(es) de parseo.")
        if (totalTrees > 0) {
            cykResult.trees.forEachIndexed { idx, tree ->
                println("\nÁrbol #${idx + 1}:")
                printParseTree(tree)
            }
        }
    } else {
        println("La oración NO es generada por la gramática.")
    }
}

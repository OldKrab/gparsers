package org.parser.neo4j

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.parser.combinators.graph.EdgeState
import org.parser.combinators.graph.GraphCombinators
import org.parser.combinators.graph.StartState
import org.parser.combinators.graph.VertexState

class DefaultNeo4jNode(id: Long, val labels: List<String>, val properties: Map<String, Any>) : Neo4jNode(id) {
    override fun toString(): String {
        val labels = labels.joinToString(", ") { "\"${it}\"" }
        val properties = properties.entries.joinToString(", ") { "\"${it.key}\"->\"${it.value}\"" }
        return "${DefaultNeo4jNode::class.simpleName}($labels) {$properties}"
    }
}

class DefaultNeo4jEdge(id: Long, val label: String, val properties: Map<String, Any>) : Neo4jEdge(id) {
    override fun toString(): String {
        val properties = properties.entries.joinToString(", ") { "\"${it.key}\"->\"${it.value}\"" }
        return "${DefaultNeo4jEdge::class.simpleName}($label) {$properties}"
    }
}

class DefaultNeo4jGraphFactory() : Neo4jGraphFactory<DefaultNeo4jNode, DefaultNeo4jEdge> {
    override fun createNode(neo4jNode: Node): DefaultNeo4jNode {
        return DefaultNeo4jNode(
            neo4jNode.id,
            neo4jNode.labels.map { it.name() }.toList(),
            neo4jNode.allProperties
        )
    }

    override fun createEdge(neo4jRelationship: Relationship): DefaultNeo4jEdge {
        return DefaultNeo4jEdge(
            neo4jRelationship.id,
            neo4jRelationship.type.name(),
            neo4jRelationship.allProperties
        )
    }
}

class DefaultNeo4jGraph(db: GraphDatabaseService): Neo4jGraph<DefaultNeo4jNode, DefaultNeo4jEdge>(db, DefaultNeo4jGraphFactory())

typealias DefaultNeo4jStartState = StartState<DefaultNeo4jNode, DefaultNeo4jEdge>
typealias DefaultNeo4jVertexState = VertexState<DefaultNeo4jNode, DefaultNeo4jEdge>
typealias DefaultNeo4jEdgeState = EdgeState<DefaultNeo4jNode, DefaultNeo4jEdge>

object DefaultNeo4jCombinators : GraphCombinators<DefaultNeo4jNode, DefaultNeo4jEdge>



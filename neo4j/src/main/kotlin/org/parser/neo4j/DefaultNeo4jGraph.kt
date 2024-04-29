package org.parser.neo4j

import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship

class DefaultNeo4jNode(id: String, val labels: List<String>, val properties: Map<String, Any>) : Neo4jNode(id) {
    override fun toString(): String {
        val labels = labels.joinToString(", ") { "\"${it}\"" }
        val properties = properties.entries.joinToString(", ") { "\"${it.key}\"->\"${it.value}\"" }
        return "${DefaultNeo4jNode::class.simpleName}($labels) {$properties}"
    }
}

class DefaultNeo4jEdge(id: String, val label: String, val properties: Map<String, Any>) : Neo4jEdge(id) {
    override fun toString(): String {
        val properties = properties.entries.joinToString(", ") { "\"${it.key}\"->\"${it.value}\"" }
        return "${DefaultNeo4jEdge::class.simpleName}($label) {$properties}"
    }
}

class DefaultNeo4jGraphFactory() : Neo4jGraphFactory<DefaultNeo4jNode, DefaultNeo4jEdge> {
    override fun createNode(neo4jNode: Node): DefaultNeo4jNode {
        return DefaultNeo4jNode(
            neo4jNode.elementId,
            neo4jNode.labels.map { it.name() }.toList(),
            neo4jNode.allProperties
        )
    }

    override fun createEdge(neo4jRelationship: Relationship): DefaultNeo4jEdge {
        return DefaultNeo4jEdge(
            neo4jRelationship.elementId,
            neo4jRelationship.type.name(),
            neo4jRelationship.allProperties
        )
    }
}

typealias DefaultNeo4jGraph = Neo4jGraph<DefaultNeo4jNode, DefaultNeo4jEdge>



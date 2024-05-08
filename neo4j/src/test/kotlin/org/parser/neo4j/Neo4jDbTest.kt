package org.parser.neo4j

import org.junit.jupiter.api.Assertions.*
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label

class Neo4jDbTest : BaseDefaultNeo4jTest() {
    val nodeName = "node name"
    var nodeId: Long = 0

    override fun fillDb(db: GraphDatabaseService) {
        nodeId = db.beginTx().let { tx ->
            val node = tx.createNode(Label.label(nodeName))
            node.setProperty("key", "value")
            tx.commit()
            node.id
        }
        val node = db.beginTx().getNodeById(nodeId)
        val labels =  node.labels.toList()
        assertTrue(node.hasProperty("key"))
        assertEquals("value", node.getProperty("key"))
        assertEquals(1, labels.size)
        assertEquals(nodeName, labels[0].name())
    }

    override fun testGraph(gr: Neo4jGraph<DefaultNeo4jNode, DefaultNeo4jEdge>) {
    }


}
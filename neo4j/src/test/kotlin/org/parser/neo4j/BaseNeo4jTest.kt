package org.parser.neo4j

import org.junit.jupiter.api.Test
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.harness.Neo4jBuilders

abstract class BaseNeo4jTest<N : Neo4jNode, E : Neo4jEdge> {
    private fun createDb(): GraphDatabaseService {
        val builder = Neo4jBuilders.newInProcessBuilder()
        return builder.build().defaultDatabaseService()
    }

    protected abstract fun fillDb(db: GraphDatabaseService)
    protected abstract fun createGraph(db: GraphDatabaseService): Neo4jGraph<N, E>
    protected abstract fun testGraph(gr: Neo4jGraph<N, E>)

    @Test
    fun test() {
        val db = createDb()
        fillDb(db)
        val gr = createGraph(db)
        testGraph(gr)
    }
}

abstract class BaseDefaultNeo4jTest: BaseNeo4jTest<DefaultNeo4jNode, DefaultNeo4jEdge>() {
    override fun createGraph(db: GraphDatabaseService): DefaultNeo4jGraph {
        return DefaultNeo4jGraph(db)
    }
}
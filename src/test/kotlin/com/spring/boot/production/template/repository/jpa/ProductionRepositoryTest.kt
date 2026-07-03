package com.spring.boot.production.template.repository.jpa

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals

@DataJpaTest
class ProductionRepositoryTest {

    @Autowired
    private lateinit var productionRepository: ProductionRepository

    @Test
    @Sql(scripts = ["/sql/feature_table.sql"])
    fun find_by_description_repository_success_test() {
        val productionRepositoryRs = productionRepository.findByDescription("Важная встреча с клиентом")

        assertEquals(1, productionRepositoryRs.size)
        assertEquals(1, productionRepositoryRs[0].id)
        assertEquals("Важная встреча с клиентом", productionRepositoryRs[0].description)
    }
}
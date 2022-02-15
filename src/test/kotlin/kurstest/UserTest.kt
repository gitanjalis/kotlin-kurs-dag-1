package kurstest

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

val testAppConfig = createAppConfig("test")
val testDataSource = createAndMigrateDataSource(testAppConfig)

fun testTx(handler: (dbSess: TransactionalSession) -> Unit) {
    sessionOf(testDataSource, returnGeneratedKey = true).use { dbSess ->
        dbSess.transaction { dbSessTx ->
            try {
                handler(dbSessTx)
            } finally {
                dbSessTx.connection.rollback()
            }
        }
    }
}

class UserTest {
    @Test fun testWhatever() {
        assertEquals(1, 1)
    }

    @Test fun testCreateUser() {
        testTx { dbSess ->
            val userId = createUser(dbSess, email = "august@crud.business", name = "August Lilleaas", passwordText = "1234")
            assertNotNull(userId)
        }
    }

    @Test fun testCreateAndGetUser() {
        testTx { dbSess ->
            val userId = createUser(dbSess, email = "august@crud.business", name = "August Lilleaas", passwordText = "1234")

            val user = getUserById(dbSess, userId)
            assertNotNull(user)
            assertEquals(userId, user.id)
            assertEquals("august@crud.business", user.email)
        }
    }

    @Test fun listUsers() {
        testTx { dbSess ->
            val userAId = createUser(dbSess, email = "august@crud.business", name = "August Lilleaas", passwordText = "1234")
            val userBId = createUser(dbSess, email = "august@augustl.com", name = "August Lilleaas", passwordText = "1234")
            val userCId = createUser(dbSess, email = "alilleaas@gmail.com", name = "August Lilleaas", passwordText = "1234")

            val users = listUsers(dbSess)
            assertEquals(3, users.size)
            assertEquals(setOf(userAId, userBId, userCId), users.map { it.id }.toSet())
        }
    }

    @Test fun testVerifyUserPassword() {
        testTx { dbSess ->
            val userId = createUser(dbSess, email = "august@crud.business", name = "August Lilleaas", passwordText = "1234")

            assertTrue(verifyUserPassword(dbSess, email = "august@crud.business", passwordText = "1234"))
            assertFalse(verifyUserPassword(dbSess, email = "august@crud.business", passwordText = "asdf"))
        }
    }
}
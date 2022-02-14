package kurstest

import kotliquery.Session
import kotliquery.queryOf

data class User(
    val id: Long,
    val email: String,
    val name: String
) {
    companion object {
        fun fromRow(row: Map<String, Any?>) = User(
            id = row["id"] as Long,
            email = row["email"] as String,
            name = row["name"] as String
        )
    }
}

fun createUser(dbSession: Session, email: String, name: String): Long {
    val userId = dbSession.updateAndReturnGeneratedKey(
        queryOf(
            "INSERT INTO user_t (email, name) VALUES (:email, :name)",
            mapOf(
                "email" to email,
                "name" to name
            )
        )
    )

    return userId!!
}


fun listUsers(dbSession: Session) =
    dbSession
        .list(queryOf("SELECT * FROM user_t"), ::mapFromRow)
        .map(User::fromRow)

fun getUserById(dbSession: Session, userId: Long?): User? {
    if (userId == null) return null

    return dbSession
        .single(queryOf("SELECT * FROM user_t WHERE id = ?", userId), ::mapFromRow)
        ?.let {
            User.fromRow(it)
        }
}
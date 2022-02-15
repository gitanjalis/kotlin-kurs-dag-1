package kurstest

import at.favre.lib.crypto.bcrypt.BCrypt
import kotliquery.Session
import kotliquery.queryOf

val bcryptHasher = BCrypt.withDefaults()
val bcryptVerifier = BCrypt.verifyer()

data class User(
    val id: Long,
    val email: String,
    val name: String,
    val passwordHash: ByteArray
) {
    companion object {
        fun fromRow(row: Map<String, Any?>) = User(
            id = row["id"] as Long,
            email = row["email"] as String,
            name = row["name"] as String,
            passwordHash = row["password_hash"] as ByteArray
        )
    }
}

fun createUser(dbSession: Session, email: String, name: String, passwordText: String): Long {
    val userId = dbSession.updateAndReturnGeneratedKey(
        queryOf(
            "INSERT INTO user_t (email, name, password_hash) VALUES (:email, :name, :passwordHash)",
            mapOf(
                "email" to email,
                "name" to name,
                "passwordHash" to bcryptHasher.hash(10, passwordText.toByteArray(Charsets.UTF_8))
            )
        )
    )

    return userId!!
}

fun verifyUserPassword(dbSession: Session, email: String, passwordText: String): Boolean {
    val pwHashRow = dbSession.single(queryOf("SELECT * FROM user_t WHERE email = ?", email), ::mapFromRow)
    if (pwHashRow == null) {
        return false
    }

    val pwHash = pwHashRow["password_hash"]!! as ByteArray

    return bcryptVerifier.verify(passwordText.toByteArray(Charsets.UTF_8), pwHash).verified
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
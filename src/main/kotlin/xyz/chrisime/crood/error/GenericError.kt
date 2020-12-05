package xyz.chrisime.crood.error

sealed class GenericError(msg: String?, th: Throwable?) : RuntimeException(msg, th) {
    class Database(msg: String? = "${ErrorTag.DATABASE.msg}: %msg", cause: Throwable? = null) : GenericError(msg, cause)

    class General(msg: String = "${ErrorTag.GENERAL.msg}: %msg", cause: Throwable? = null) : GenericError(msg, cause)
}

enum class ErrorTag(val msg: String) {
    GENERAL("general error"),
    DATABASE("database error"),
}

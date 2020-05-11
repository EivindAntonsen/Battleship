package no.esa.battleship.repository.exceptions

data class DataAccessException(override val message: String,
                               val javaClass: Class<*>,
                               override val cause: Throwable?) : RuntimeException(message)

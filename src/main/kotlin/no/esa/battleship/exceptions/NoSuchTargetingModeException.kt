package no.esa.battleship.exceptions

class NoSuchTargetingModeException(id: Int? = null) : RuntimeException(id.let {
    if (id != null) {
        "No targeting mode for id $id!"
    } else "No targeting mode found!"
})

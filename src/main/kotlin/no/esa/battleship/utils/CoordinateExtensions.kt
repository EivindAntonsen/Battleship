package no.esa.battleship.utils

import no.esa.battleship.repository.entity.CoordinateEntity

infix fun CoordinateEntity.isVerticallyAlignedWith(that: CoordinateEntity): Boolean {
    return this.horizontalPosition == that.horizontalPosition
}

infix fun CoordinateEntity.isHorizontallyAlignedWith(that: CoordinateEntity): Boolean {
    return this.verticalPosition == that.verticalPosition
}

infix fun CoordinateEntity.isAdjacentWith(that: CoordinateEntity): Boolean {
    val theyAreHorizontallyAligned = this isHorizontallyAlignedWith that
    val theyAreVerticallyAligned = this isVerticallyAlignedWith that
    val distanceHorizontallyApartIsOne = this.horizontalPositionAsInt() - that.horizontalPositionAsInt() in listOf(-1, 1)
    val distanceVerticallyApartIsOne = this.verticalPosition - that.verticalPosition in listOf(-1, 1)


    return theyAreHorizontallyAligned && distanceHorizontallyApartIsOne ||
            theyAreVerticallyAligned && distanceVerticallyApartIsOne
}

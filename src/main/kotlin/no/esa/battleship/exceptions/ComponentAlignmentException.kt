package no.esa.battleship.exceptions

class ComponentAlignmentException(horizontalCount: Int,
                                  verticalCount: Int) : RuntimeException("Invalid alignment of coordinates: " +
                                                                                 "$horizontalCount horizontal coordinates and " +
                                                                                 "$verticalCount vertical coordinates!")

package com.example.sheetlite.data

import androidx.room.Entity

@Entity(tableName = "cells", primaryKeys = ["row", "col"])
data class CellEntity(
    val row: Int,
    val col: Int,
    val rawValue: String
)

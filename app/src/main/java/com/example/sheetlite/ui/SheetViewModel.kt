package com.example.sheetlite.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sheetlite.data.AppDatabase
import com.example.sheetlite.data.CellEntity
import com.example.sheetlite.engine.FormulaEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SheetUiState(
    val rowCount: Int = 50,
    val colCount: Int = 26,
    val selectedRow: Int = 0,
    val selectedCol: Int = 0,
    val formulaInput: String = "",
    val rawCells: Map<Pair<Int, Int>, String> = emptyMap(),
    val evaluatedCells: Map<Pair<Int, Int>, String> = emptyMap()
)

class SheetViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).cellDao()

    private val selectedCell = MutableStateFlow(Pair(0, 0))
    private val formulaInput = MutableStateFlow("")

    val uiState: StateFlow<SheetUiState> = combine(
        dao.getAllCells(),
        selectedCell,
        formulaInput
    ) { cells, selected, input ->
        val rawMap = cells.associate { Pair(it.row, it.col) to it.rawValue }
        val evalMap = rawMap.keys.associateWith { (r, c) ->
            FormulaEngine.evaluate(r, c, rawMap)
        }
        SheetUiState(
            selectedRow = selected.first,
            selectedCol = selected.second,
            formulaInput = input,
            rawCells = rawMap,
            evaluatedCells = evalMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SheetUiState()
    )

    fun selectCell(row: Int, col: Int) {
        commitCurrentEdit()
        selectedCell.value = Pair(row, col)
        formulaInput.value = uiState.value.rawCells[Pair(row, col)] ?: ""
    }

    fun onFormulaInputChange(newValue: String) {
        formulaInput.value = newValue
        saveCell(selectedCell.value.first, selectedCell.value.second, newValue)
    }

    fun commitCurrentEdit() {
        val (r, c) = selectedCell.value
        saveCell(r, c, formulaInput.value)
    }

    fun clearAll() {
        viewModelScope.launch {
            dao.clearAll()
            formulaInput.value = ""
        }
    }

    private fun saveCell(row: Int, col: Int, value: String) {
        viewModelScope.launch {
            if (value.isEmpty()) {
                dao.deleteCell(row, col)
            } else {
                dao.upsertCell(CellEntity(row = row, col = col, rawValue = value))
            }
        }
    }
}

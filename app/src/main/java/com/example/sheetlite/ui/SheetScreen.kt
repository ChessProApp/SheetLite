package com.example.sheetlite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sheetlite.engine.FormulaEngine

private val HeaderBg = Color(0xFFF1F5F9)
private val SelectedCellBg = Color(0xFFE8F5E9)
private val ActiveBorder = Color(0xFF1B5E20)
private val GridBorder = Color(0xFFCBD5E1)

private val RowHeaderWidth = 48.dp
private val CellWidth = 96.dp
private val CellHeight = 36.dp

@Composable
fun SheetScreen(viewModel: SheetViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val horizontalScroll = rememberScrollState()
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar & Formel-Leiste
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color(0xFF1B5E20))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SheetLite",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.clearAll() }) {
                    Text(text = "Leeren", color = Color.White, fontSize = 13.sp)
                }
            }

            // Kompakt-Formelleiste
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                        .background(HeaderBg, shape = MaterialTheme.shapes.extraSmall)
                        .border(1.dp, GridBorder, shape = MaterialTheme.shapes.extraSmall),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = FormulaEngine.cellCoordsToName(state.selectedRow, state.selectedCol),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ActiveBorder
                    )
                }

                Text(
                    text = "fx",
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                        .background(Color.White, shape = MaterialTheme.shapes.extraSmall)
                        .border(1.dp, GridBorder, shape = MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (state.formulaInput.isEmpty()) {
                        Text(
                            text = "Wert oder =SUM(A1:A5) eingeben...",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = state.formulaInput,
                        onValueChange = viewModel::onFormulaInputChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(ActiveBorder),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.commitCurrentEdit()
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HorizontalDivider(color = GridBorder, thickness = 1.dp)

            // Fixierte Spalten-Header (A, B, C...)
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(RowHeaderWidth)
                        .height(CellHeight)
                        .background(HeaderBg)
                        .border(0.5.dp, GridBorder)
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                ) {
                    for (col in 0 until state.colCount) {
                        val isColSelected = col == state.selectedCol
                        Box(
                            modifier = Modifier
                                .width(CellWidth)
                                .height(CellHeight)
                                .background(if (isColSelected) SelectedCellBg else HeaderBg)
                                .border(0.5.dp, GridBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = FormulaEngine.colIndexToName(col),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isColSelected) ActiveBorder else Color.DarkGray
                            )
                        }
                    }
                }
            }

            // 2D-Grid mit fixierten Zeilen-Headern (1, 2, 3...)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.rowCount, key = { it }) { row ->
                    val isRowSelected = row == state.selectedRow
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Fixierter Zeilen-Header
                        Box(
                            modifier = Modifier
                                .width(RowHeaderWidth)
                                .height(CellHeight)
                                .background(if (isRowSelected) SelectedCellBg else HeaderBg)
                                .border(0.5.dp, GridBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${row + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isRowSelected) ActiveBorder else Color.DarkGray
                            )
                        }

                        // Horizontal scrollbare Zellen der Zeile
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScroll)
                        ) {
                            for (col in 0 until state.colCount) {
                                val isSelected = row == state.selectedRow && col == state.selectedCol
                                val displayText = state.evaluatedCells[Pair(row, col)] ?: ""
                                val isNumber = displayText.toDoubleOrNull() != null
                                val isError = displayText.startsWith("#")

                                Box(
                                    modifier = Modifier
                                        .width(CellWidth)
                                        .height(CellHeight)
                                        .background(if (isSelected) SelectedCellBg else Color.White)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) ActiveBorder else GridBorder
                                        )
                                        .clickable { viewModel.selectCell(row, col) }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = if (isNumber) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Text(
                                        text = displayText,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = if (isNumber) TextAlign.End else TextAlign.Start,
                                        color = when {
                                            isError -> Color.Red
                                            isNumber -> Color(0xFF0F172A)
                                            else -> Color(0xFF1E293B)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

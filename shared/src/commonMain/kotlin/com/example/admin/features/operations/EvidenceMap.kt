package com.example.admin.features.operations

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
@Composable expect fun EvidenceMap(data:String,modifier:Modifier=Modifier,onSelect:(String)->Unit={})

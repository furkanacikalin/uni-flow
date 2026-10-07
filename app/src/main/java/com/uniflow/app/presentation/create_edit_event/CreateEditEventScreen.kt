package com.uniflow.app.presentation.create_edit_event

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalFocusManager

import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditEventScreen(
    clubId: String?,
    eventId: String?,
    onNavigateBack: () -> Unit,
    viewModel: CreateEditEventViewModel = hiltViewModel()
) {
    val state = viewModel.state.value
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val categories = listOf("Teknoloji & Yazılım", "Kültür & Sanat", "Spor & Sağlık", "Akademik & Kariyer", "Sosyal & Eğlence")

    LaunchedEffect(clubId, eventId) {
        viewModel.init(clubId, eventId)
    }

    LaunchedEffect(state.isSavedSuccess) {
        if (state.isSavedSuccess) {
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error.isNotBlank()) {
            Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (state.isEditMode) "Etkinliği Düzenle" else "Yeni Etkinlik Oluştur",
                            fontWeight = FontWeight.Bold
                        )
                        if (state.clubName.isNotBlank()) {
                            Text(
                                text = state.clubName,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        focusManager.clearFocus()
                        onNavigateBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                }
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "📌 Etkinlik Bilgileri",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Öğrencilere duyurmak istediğiniz etkinliğin tüm detaylarını aşağıda belirtebilirsiniz. Tarih ve saat alanları gerçek takvim/saat seçimi ile yapılır.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }


                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            OutlinedTextField(
                                value = state.title,
                                onValueChange = viewModel::onTitleChange,
                                label = { Text("Etkinlik Başlığı *") },
                                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, tint = Color(0xFF1D4ED8)) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )


                            OutlinedTextField(
                                value = state.description,
                                onValueChange = viewModel::onDescriptionChange,
                                label = { Text("Etkinlik Açıklaması") },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF1D4ED8)) },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )


                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Etkinlik Kategorisi *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.forEach { cat ->
                                        val isSelected = state.category == cat
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                focusManager.clearFocus()
                                                viewModel.onCategoryChange(cat)
                                            },
                                            label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                                        )
                                    }
                                }
                            }


                            OutlinedTextField(
                                value = state.location,
                                onValueChange = viewModel::onLocationChange,
                                label = { Text("Konum / Mekan") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF1D4ED8)) },
                                placeholder = { Text("Örn: Mühendislik Amfisi A-101") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9))


                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Etkinlik Tarihi *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            focusManager.clearFocus()
                                            viewModel.openDatePicker()
                                        },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF1D4ED8))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = state.dateText.ifBlank { "Tarih Seçmek İçin Dokunun..." },
                                                fontSize = 14.sp,
                                                fontWeight = if (state.dateText.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                                color = if (state.dateText.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                            )
                                        }
                                        Text("📅 Seç", fontSize = 12.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }


                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Etkinlik Saatleri *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {

                                    OutlinedCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                focusManager.clearFocus()
                                                viewModel.openStartTimePicker()
                                            },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Text("Başlangıç", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = state.startTimeText,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        }
                                    }


                                    OutlinedCard(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                focusManager.clearFocus()
                                                viewModel.openEndTimePicker()
                                            },
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Text("Bitiş", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = state.endTimeText,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))


                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.saveEvent()
                                },
                                enabled = !state.isSaving,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (state.isEditMode) "Etkinliği Güncelle" else "Yeni Etkinliği Yayınla",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val todayUtcMidnight = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        val localCal = Calendar.getInstance()
                        set(Calendar.YEAR, localCal.get(Calendar.YEAR))
                        set(Calendar.MONTH, localCal.get(Calendar.MONTH))
                        set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    return utcTimeMillis >= todayUtcMidnight
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year >= Calendar.getInstance().get(Calendar.YEAR)
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.closeDatePicker() },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            viewModel.onDateSelected(millis)
                        } else {
                            viewModel.closeDatePicker()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                ) {
                    Text("Seç")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeDatePicker() }) {
                    Text("İptal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }


    if (state.showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.startHour,
            initialMinute = state.startMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { viewModel.closeStartTimePicker() },
            title = { Text("Başlangıç Saati Seçin", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onStartTimeSelected(timePickerState.hour, timePickerState.minute)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                ) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeStartTimePicker() }) {
                    Text("İptal")
                }
            }
        )
    }


    if (state.showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = state.endHour,
            initialMinute = state.endMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { viewModel.closeEndTimePicker() },
            title = { Text("Bitiş Saati Seçin", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onEndTimeSelected(timePickerState.hour, timePickerState.minute)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                ) {
                    Text("Tamam")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeEndTimePicker() }) {
                    Text("İptal")
                }
            }
        )
    }
}

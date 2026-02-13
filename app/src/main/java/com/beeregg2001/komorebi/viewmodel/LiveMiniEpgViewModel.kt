package com.beeregg2001.komorebi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beeregg2001.komorebi.data.model.EpgProgram
import com.beeregg2001.komorebi.data.repository.EpgRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveMiniEpgViewModel @Inject constructor(
    private val epgRepository: EpgRepository
) : ViewModel() {
    private val _programs = MutableStateFlow<List<EpgProgram>>(emptyList())
    val programs: StateFlow<List<EpgProgram>> = _programs.asStateFlow()

    fun fetchNowAndNextPrograms(channelId: String) {
        viewModelScope.launch {
            epgRepository.fetchNowAndNextPrograms(channelId)
                .onSuccess { _programs.value = it }
                .onFailure { _programs.value = emptyList() }
        }
    }
}

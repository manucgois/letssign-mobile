package br.com.fiap.letssign.ui.screens.home

import androidx.lifecycle.ViewModel
import br.com.fiap.letssign.data.local.entity.UserEntity
import br.com.fiap.letssign.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser

    fun logout() = authRepository.logout()
}
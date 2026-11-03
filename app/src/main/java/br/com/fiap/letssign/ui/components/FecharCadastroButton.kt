package br.com.fiap.letssign.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.fiap.letssign.domain.repository.AuthRepository
import br.com.fiap.letssign.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel pequeno, só para descartar o rascunho a partir de qualquer etapa. */
@HiltViewModel
class RascunhoAcoesViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val documentRepository: DocumentRepository
) : ViewModel() {

    fun descartar(documentId: Long, onConcluido: () -> Unit) {
        val adminId = authRepository.currentUser.value?.id ?: return
        viewModelScope.launch {
            documentRepository.deleteDraft(adminId, documentId).onSuccess { onConcluido() }
        }
    }
}

/**
 * Botão ✕ das etapas do cadastro: sai direto para o painel,
 * perguntando antes se quer salvar o rascunho ou descartar.
 */
@Composable
fun FecharCadastroButton(
    documentId: Long,
    onSairParaPainel: () -> Unit,
    viewModel: RascunhoAcoesViewModel = hiltViewModel()
) {
    var perguntando by remember { mutableStateOf(false) }

    IconButton(onClick = { perguntando = true }) {
        Icon(Icons.Outlined.Close, contentDescription = "Fechar cadastro")
    }

    if (perguntando) {
        AlertDialog(
            onDismissRequest = { perguntando = false },
            title = { Text("Sair do cadastro?") },
            text = {
                Text("O documento fica salvo como rascunho, e você pode continuar depois pelo painel.")
            },
            confirmButton = {
                Button(onClick = {
                    perguntando = false
                    onSairParaPainel()
                }) { Text("Salvar e sair") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        perguntando = false
                        viewModel.descartar(documentId, onSairParaPainel)
                    }) {
                        Text("Descartar", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { perguntando = false }) { Text("Continuar editando") }
                }
            }
        )
    }
}
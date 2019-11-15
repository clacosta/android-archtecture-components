package br.com.alura.technews.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import br.com.alura.technews.model.Noticia
import br.com.alura.technews.repository.FalhaResource
import br.com.alura.technews.repository.NoticiaRepository
import br.com.alura.technews.repository.Resource

class VisualizaNoticiaViewModel(
    private val id: Long,
    private val repository: NoticiaRepository
) : ViewModel() {

    private val noticiaEncontrada = buscaPorId()

    fun buscaPorId(): LiveData<Noticia?> {
        return repository.buscaPorId(id)
    }

    fun remove(): LiveData<Resource<Void?>> {
        return noticiaEncontrada.value?.run {
            repository.remove(this)
        } ?: MutableLiveData<Resource<Void?>>().also { liveData ->
            liveData.value = FalhaResource(null, "Notícia não encontrada")
        }
    }

}
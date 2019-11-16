package br.com.alura.technews.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import br.com.alura.technews.asynctask.BaseAsyncTask
import br.com.alura.technews.database.dao.NoticiaDAO
import br.com.alura.technews.model.Noticia
import br.com.alura.technews.retrofit.webclient.NoticiaWebClient

class NoticiaRepository(
    private val dao: NoticiaDAO,
    private val webclient: NoticiaWebClient = NoticiaWebClient()
) {

    private val mediador = MediatorLiveData<Resource<List<Noticia>?>>()

    fun buscaTodos(): LiveData<Resource<List<Noticia>?>> {
        mediador.addSource(buscaInterno()) { noticias ->
            mediador.value = SucessoResource(dado = noticias)
        }
        val falhasDaWebApiLiveData = MutableLiveData<Resource<List<Noticia>?>>()
        mediador.addSource(falhasDaWebApiLiveData, Observer { resourceDeFalha ->
            val resourceAtual = mediador.value
            val resourceNovo =
                retornaResourceNovo(resourceAtual, resourceDeFalha)
            mediador.value = resourceNovo
        })
        buscaNaApi(quandoFalha = { erro ->
            falhasDaWebApiLiveData.value = FalhaResource(null, erro)
        })
        return mediador
    }

    private fun retornaResourceNovo(
        resourceAtual: Resource<List<Noticia>?>?,
        resourceDeFalha: Resource<List<Noticia>?>
    ): Resource<List<Noticia>?> {
        if (resourceAtual != null) {
            return FalhaResource(resourceAtual.dado, resourceDeFalha.erro)
        }
        return resourceDeFalha
    }

    fun salva(
        noticia: Noticia
    ): LiveData<Resource<Void?>> {
        val liveData = MutableLiveData<Resource<Void?>>()
        salvaNaApi(noticia,
            quandoSucesso = {
                liveData.value = SucessoResource(null)
            }, quandoFalha = { erro ->
                liveData.value = FalhaResource(null, erro)
            })
        return liveData
    }

    fun remove(
        noticia: Noticia
    ): LiveData<Resource<Void?>> {
        val liveData = MutableLiveData<Resource<Void?>>()
        removeNaApi(noticia,
            quandoSucesso = {
                liveData.value = SucessoResource(null)
            }, quandoFalha = { erro ->
                liveData.value = FalhaResource(null, erro)
            })
        return liveData
    }

    fun edita(
        noticia: Noticia
    ): LiveData<Resource<Void?>> {
        val liveData = MutableLiveData<Resource<Void?>>()
        editaNaApi(noticia,
            quandoSucesso = {
                liveData.value = SucessoResource(null)
            },
            quandoFalha = { erro ->
                liveData.value = FalhaResource(null, erro)
            })
        return liveData
    }

    fun buscaPorId(
        noticiaId: Long
    ): LiveData<Noticia?> {
        return dao.buscaPorId(noticiaId)
    }

    private fun buscaNaApi(
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.buscaTodas(
            quandoSucesso = { noticiasNovas ->
                noticiasNovas?.let {
                    salvaInterno(noticiasNovas)
                }
            }, quandoFalha = quandoFalha
        )
    }

    private fun buscaInterno(): LiveData<List<Noticia>> {
        return dao.buscaTodos()
    }

    private fun salvaNaApi(
        noticia: Noticia,
        quandoSucesso: () -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.salva(
            noticia,
            quandoSucesso = {
                it?.let { noticiaSalva ->
                    salvaInterno(noticiaSalva, quandoSucesso)
                }
            },
            quandoFalha = quandoFalha
        )
    }

    private fun salvaInterno(
        noticias: List<Noticia>
    ) {
        BaseAsyncTask(
            quandoExecuta = {
                dao.salva(noticias)
            }, quandoFinaliza = {}
        ).execute()
    }

    private fun salvaInterno(
        noticia: Noticia,
        quandoSucesso: () -> Unit
    ) {
        BaseAsyncTask(quandoExecuta = {
            dao.salva(noticia)
        }, quandoFinaliza = {
            quandoSucesso()
        }).execute()
    }

    private fun removeNaApi(
        noticia: Noticia,
        quandoSucesso: () -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.remove(
            noticia.id,
            quandoSucesso = {
                removeInterno(noticia, quandoSucesso)
            },
            quandoFalha = quandoFalha
        )
    }


    private fun removeInterno(
        noticia: Noticia,
        quandoSucesso: () -> Unit
    ) {
        BaseAsyncTask(quandoExecuta = {
            dao.remove(noticia)
        }, quandoFinaliza = {
            quandoSucesso()
        }).execute()
    }

    private fun editaNaApi(
        noticia: Noticia,
        quandoSucesso: () -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.edita(
            noticia.id, noticia,
            quandoSucesso = { noticiaEditada ->
                noticiaEditada?.let {
                    salvaInterno(noticiaEditada, quandoSucesso)
                }
            },
            quandoFalha = quandoFalha
        )
    }

}

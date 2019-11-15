package br.com.alura.technews.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import br.com.alura.technews.asynctask.BaseAsyncTask
import br.com.alura.technews.database.dao.NoticiaDAO
import br.com.alura.technews.model.Noticia
import br.com.alura.technews.retrofit.webclient.NoticiaWebClient

class NoticiaRepository(
    private val dao: NoticiaDAO,
    private val webclient: NoticiaWebClient = NoticiaWebClient()
) {

    private val noticiasEncontradas = MutableLiveData<Resource<List<Noticia>?>>()

    fun buscaTodos(): LiveData<Resource<List<Noticia>?>> {
        buscaInterno(quandoSucesso = { noticias ->
            quandoSucessoInterno(noticias)
        })
        buscaNaApi(quandoSucesso = { noticias ->
            quandoSucessoNaApi(noticias)
        }, quandoFalha = { erro ->
            quandoFalhaNaApi(erro)
        }
        )
        return noticiasEncontradas
    }

    private fun quandoSucessoInterno(noticias: List<Noticia>) {
        noticiasEncontradas.value = Resource(dado = noticias)
    }

    private fun quandoSucessoNaApi(noticias: List<Noticia>) {
        noticiasEncontradas.value = SucessoResource(dado = noticias)
    }

    private fun quandoFalhaNaApi(erro: String?) {
        val resourseAtual = noticiasEncontradas.value
        noticiasEncontradas.value = FalhaResource(resourseAtual?.dado, erro)
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
        val liveData = MutableLiveData<Noticia?>()
        BaseAsyncTask(quandoExecuta = {
            dao.buscaPorId(noticiaId)
        }, quandoFinaliza = { noticia ->
            liveData.value = noticia
        })
            .execute()
        return liveData
    }

    private fun buscaNaApi(
        quandoSucesso: (List<Noticia>) -> Unit,
        quandoFalha: (erro: String?) -> Unit
    ) {
        webclient.buscaTodas(
            quandoSucesso = { noticiasNovas ->
                noticiasNovas?.let {
                    salvaInterno(noticiasNovas, quandoSucesso)
                }
            }, quandoFalha = quandoFalha
        )
    }

    private fun buscaInterno(quandoSucesso: (List<Noticia>) -> Unit) {
        BaseAsyncTask(quandoExecuta = {
            dao.buscaTodos()
        }, quandoFinaliza = { noticias ->
            quandoSucesso(noticias)
        }).execute()
    }

    private fun salvaNaApi(
        noticia: Noticia,
        quandoSucesso: (noticiaNova: Noticia) -> Unit,
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
        noticias: List<Noticia>,
        quandoSucesso: (noticiasNovas: List<Noticia>) -> Unit
    ) {
        BaseAsyncTask(
            quandoExecuta = {
                dao.salva(noticias)
                dao.buscaTodos()
            }, quandoFinaliza = quandoSucesso
        ).execute()
    }

    private fun salvaInterno(
        noticia: Noticia,
        quandoSucesso: (noticiaNova: Noticia) -> Unit
    ) {
        BaseAsyncTask(quandoExecuta = {
            dao.salva(noticia)
            dao.buscaPorId(noticia.id)
        }, quandoFinaliza = { noticiaEncontrada ->
            noticiaEncontrada?.let { noticia ->
                quandoSucesso(noticia)
            }
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
        quandoSucesso: (noticiaEditada: Noticia) -> Unit,
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

package br.com.alura.technews.repository

class Resourse<T>(
    val dado: T,
    val erro: String? = null
)

fun <T> criaResourceDeFalha(
    resourseAtual: Resourse<T?>?,
    erro: String?
): Resourse<T?> {
    if (resourseAtual != null) {
        return Resourse(dado = resourseAtual.dado, erro = erro)
    }
    return Resourse(dado = null, erro = erro)
}
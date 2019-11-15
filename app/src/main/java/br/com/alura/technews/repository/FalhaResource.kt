package br.com.alura.technews.repository

class FalhaResource<T>(dado: T?, erro: String?) : Resource<T>(dado = null, erro = erro)
package uk.co.jcox.snake

interface BindableState {
    fun bind()

    fun unbind()

    fun destroy()
}
package model

@JvmInline
value class Rank(val value: Int) {
    init {
        require(value in 1..5) { "Rank must be between 1 and 5" }
    }

    fun next(): Rank? = if (value < 5) Rank(value + 1) else null

    override fun toString(): String = value.toString()
}

package com.vaycore.finance.util.deivce

/** Collects one named section of the risk snapshot payload. */
internal interface RiskSectionCollector {
    val key: String

    suspend fun collect(): Any?

    fun fallbackValue(): Any? = null
}

/** Adapts a section-producing lambda to the risk snapshot collector contract. */
internal class LambdaRiskSectionCollector(
    override val key: String,
    private val fallback: () -> Any? = { null },
    private val block: suspend () -> Any?
) : RiskSectionCollector {

    override suspend fun collect(): Any? = block()

    override fun fallbackValue(): Any? = fallback()
}

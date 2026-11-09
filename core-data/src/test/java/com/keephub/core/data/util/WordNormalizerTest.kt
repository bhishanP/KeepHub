package com.keephub.core.data.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WordNormalizerTest {

    private val normalizer = WordNormalizer()

    @Test
    fun `strips diacritics and case`() {
        assertThat(normalizer.normalizeForKey("Café")).isEqualTo("cafe")
        assertThat(normalizer.normalizeForKey("résumé")).isEqualTo("resume")
        assertThat(normalizer.normalizeForKey("NAÏVE")).isEqualTo("naive")
    }

    @Test
    fun `collapses hyphens and whitespace for compound words`() {
        assertThat(normalizer.normalizeForKey("well-being")).isEqualTo("wellbeing")
        assertThat(normalizer.normalizeForKey("well being")).isEqualTo("wellbeing")
        assertThat(normalizer.normalizeForKey("wellbeing")).isEqualTo("wellbeing")
        assertThat(normalizer.normalizeForKey("ice-cream")).isEqualTo("icecream")
        assertThat(normalizer.normalizeForKey("ice cream")).isEqualTo("icecream")
    }

    @Test
    fun `strips grammatical prefixes like to, a, an, the`() {
        assertThat(normalizer.normalizeForKey("to make")).isEqualTo("make")
        assertThat(normalizer.normalizeForKey("a phenomenon")).isEqualTo("phenomenon")
        assertThat(normalizer.normalizeForKey("an apple")).isEqualTo("apple")
        assertThat(normalizer.normalizeForKey("the government")).isEqualTo("government")
    }

    @Test
    fun `handles silent e drop for ing and ed`() {
        // making -> make
        assertThat(normalizer.normalizeForKey("making")).isEqualTo("make")
        // dancing -> dance
        assertThat(normalizer.normalizeForKey("dancing")).isEqualTo("dance")
        // writing -> write
        assertThat(normalizer.normalizeForKey("writing")).isEqualTo("write")
        // baked -> bake
        assertThat(normalizer.normalizeForKey("baked")).isEqualTo("bake")
        // created -> create
        assertThat(normalizer.normalizeForKey("created")).isEqualTo("create")
    }

    @Test
    fun `handles doubled consonants`() {
        // running -> run
        assertThat(normalizer.normalizeForKey("running")).isEqualTo("run")
        // stopped -> stop
        assertThat(normalizer.normalizeForKey("stopped")).isEqualTo("stop")
        // planned -> plan
        assertThat(normalizer.normalizeForKey("planning")).isEqualTo("plan")
    }

    @Test
    fun `handles plurals and regular inflections`() {
        assertThat(normalizer.normalizeForKey("cats")).isEqualTo("cat")
        assertThat(normalizer.normalizeForKey("boxes")).isEqualTo("box")
        assertThat(normalizer.normalizeForKey("studies")).isEqualTo("study")
        assertThat(normalizer.normalizeForKey("wolves")).isEqualTo("wolf")
    }

    @Test
    fun `handles adverbs and comparatives`() {
        assertThat(normalizer.normalizeForKey("happily")).isEqualTo("happy")
        assertThat(normalizer.normalizeForKey("happier")).isEqualTo("happy")
        assertThat(normalizer.normalizeForKey("happiest")).isEqualTo("happy")
        assertThat(normalizer.normalizeForKey("quicker")).isEqualTo("quick")
        assertThat(normalizer.normalizeForKey("quickest")).isEqualTo("quick")
    }

    @Test
    fun `candidate keys include root and variants`() {
        val candidates = normalizer.candidateKeys("making")
        assertThat(candidates).contains("make")
        assertThat(candidates).contains("making")

        val compoundCandidates = normalizer.candidateKeys("well-being")
        assertThat(compoundCandidates).contains("wellbeing")
    }
}

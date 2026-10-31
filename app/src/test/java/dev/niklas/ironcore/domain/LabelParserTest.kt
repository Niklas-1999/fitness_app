package dev.niklas.ironcore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelParserTest {
    private fun eq(exp: Float, act: Float?) = assertEquals(exp, act!!, .01f)

    @Test fun germanOats() {
        val v = LabelParser.parse("""
            Nährwerte pro 100 g
            Brennwert 1556 kJ / 372 kcal
            Fett 7,0 g
            davon gesättigte Fettsäuren 1,3 g
            Kohlenhydrate 58,7 g
            davon Zucker 0,7 g
            Ballaststoffe 10 g
            Eiweiß 13,5 g
            Salz 0,01 g
        """.trimIndent())
        eq(372f, v.kcal); eq(7f, v.fat); eq(1.3f, v.satFat); eq(58.7f, v.carbs); eq(.7f, v.sugar)
        eq(10f, v.fibre); eq(13.5f, v.protein); eq(.01f, v.salt)
        assertEquals(8, v.found); assertTrue(v.consistent)
    }

    @Test fun twoColumnsAndReferenceIntake() {
        // per 100 g | per portion (30 g) | %RI*
        val v = LabelParser.parse("""
            NUTRITION INFORMATION   per 100g   per 30g portion   %RI*
            Energy   1650kJ   495kJ
                     392kcal  118kcal   6%
            Fat      2.1g     0.6g      1%
            of which saturates  0.4g  0.1g  1%
            Carbohydrate  84g  25g  10%
            of which sugars  8.0g  2.4g  3%
            Fibre  3.0g  0.9g
            Protein  7.5g  2.3g  5%
            Salt  1.13g  0.34g  6%
        """.trimIndent())
        eq(392f, v.kcal); eq(2.1f, v.fat); eq(.4f, v.satFat); eq(84f, v.carbs); eq(8f, v.sugar)
        eq(3f, v.fibre); eq(7.5f, v.protein); eq(1.13f, v.salt)
    }

    @Test fun ocrNoiseAndLessThan() {
        val v = LabelParser.parse("""
            Durchschnittliche Nährwerte je 100 ml
            Energie 263 kJ/ 63 kcal
            Fett O,2 g
            - davon gesattigte Fettsauren O,l g
            Kohlenhydrate 4,O g
            - davon Zucker 4,0 g
            Eiweiss 11 g
            Salz <0,1 g
        """.trimIndent())
        eq(63f, v.kcal); eq(.2f, v.fat); eq(4f, v.carbs); eq(4f, v.sugar); eq(11f, v.protein); eq(.1f, v.salt)
    }

    @Test fun kilojouleOnlyAndMissingFields() {
        val v = LabelParser.parse("Energie 2000 kJ\nFett 20 g\nEiweiß 5 g")
        eq(478f, v.kcal?.let { kotlin.math.round(it) })
        assertNull(v.carbs); assertNull(v.salt)
        assertEquals(3, v.found)
    }

    @Test fun saltWordBoundaryAndSatFatSanity() {
        val v = LabelParser.parse("Selen 30 µg\nFett 3 g\ndavon gesättigte Fettsäuren 9 g\nSalz 0,5 g")
        eq(.5f, v.salt)
        assertNull("saturates > fat is rejected", v.satFat)
    }

    /** Real Tesseract output from the on-device test scan: "g" read as "9", "kJ" as "k]". */
    @Test fun gReadAsNine() {
        val v = LabelParser.parse("""
            Nährwertangaben / Nutrition information

            pro 100g pro 409

            Brennwert                          1590 k]

            636 kJ

            376 kcal

            150 kcal

            Fett                                      6,99          2,89
            davon gesattigte Fettsauren       1,29         0,59
            Kohlenhydrate                      60g          24g
            davon Zucker                       1,19         0,49
            Ballaststoffe                         9,79         3,99
            Eiweiß                               139          5,29
            Salz                                  0,029        <0,019
        """.trimIndent())
        eq(376f, v.kcal); eq(6.9f, v.fat); eq(1.2f, v.satFat); eq(60f, v.carbs); eq(1.1f, v.sugar)
        eq(9.7f, v.fibre); eq(13f, v.protein); eq(.02f, v.salt)
        assertTrue(v.consistent)
    }

    @Test fun genuineNinesAreKept() {
        val v = LabelParser.parse("Fett 9 g 3,9 g\nEiweiß 19 g\nKohlenhydrate 49,9 g")
        eq(9f, v.fat); eq(19f, v.protein); eq(49.9f, v.carbs)
    }
}

package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class CategoryColorTheme(
    val primary: Color,
    val container: Color,
    val onContainer: Color,
    val gradient: Brush
)

object CategoryColors {
    val WhiskyTheme = CategoryColorTheme(
        primary = Color(0xFFD97706), // Warm Amber
        container = Color(0xFFFEF3C7),
        onContainer = Color(0xFF78350F),
        gradient = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
    )

    val BeerTheme = CategoryColorTheme(
        primary = Color(0xFFEAB308), // Crisp Golden Yellow
        container = Color(0xFFFEF9C3),
        onContainer = Color(0xFF713F12),
        gradient = Brush.horizontalGradient(listOf(Color(0xFFFACC15), Color(0xFFCA8A04)))
    )

    val BrandyTheme = CategoryColorTheme(
        primary = Color(0xFFE11D48), // Rich Royal Ruby / Cognac
        container = Color(0xFFFFE4E6),
        onContainer = Color(0xFF881337),
        gradient = Brush.horizontalGradient(listOf(Color(0xFFF43F5E), Color(0xFFBE123C)))
    )

    val RumTheme = CategoryColorTheme(
        primary = Color(0xFFEA580C), // Spiced Bronze / Mahogany
        container = Color(0xFFFFEDD5),
        onContainer = Color(0xFF7C2D12),
        gradient = Brush.horizontalGradient(listOf(Color(0xFFFB923C), Color(0xFFC2410C)))
    )

    val VodkaTheme = CategoryColorTheme(
        primary = Color(0xFF0891B2), // Glacier Sapphire / Cyan
        container = Color(0xFFCFFAFE),
        onContainer = Color(0xFF164E63),
        gradient = Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0E7490)))
    )

    val WineTheme = CategoryColorTheme(
        primary = Color(0xFF9333EA), // Royal Bordeaux / Plum
        container = Color(0xFFF3E8FF),
        onContainer = Color(0xFF581C87),
        gradient = Brush.horizontalGradient(listOf(Color(0xFFA855F7), Color(0xFF7E22CE)))
    )

    val DefaultTheme = CategoryColorTheme(
        primary = Color(0xFF4F46E5), // Indigo
        container = Color(0xFFEEF2FF),
        onContainer = Color(0xFF312E81),
        gradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF4338CA)))
    )

    fun forCategory(category: String): CategoryColorTheme {
        return when (category.lowercase()) {
            "whisky", "whiskey" -> WhiskyTheme
            "beer" -> BeerTheme
            "brandy" -> BrandyTheme
            "rum" -> RumTheme
            "vodka" -> VodkaTheme
            "wine" -> WineTheme
            else -> DefaultTheme
        }
    }

    fun forBottleSize(size: String): Pair<Color, Color> {
        return when {
            size.contains("750") -> Color(0xFF7C3AED) to Color(0xFFEDE9FE)
            size.contains("375") -> Color(0xFF0284C7) to Color(0xFFE0F2FE)
            size.contains("180") -> Color(0xFF059669) to Color(0xFFD1FAE5)
            size.contains("650") -> Color(0xFFD97706) to Color(0xFFFEF3C7)
            size.contains("1000") || size.contains("1L") -> Color(0xFFDB2777) to Color(0xFFFCE7F3)
            size.contains("500") -> Color(0xFF2563EB) to Color(0xFFDBEAFE)
            else -> Color(0xFF4B5563) to Color(0xFFF3F4F6)
        }
    }
}

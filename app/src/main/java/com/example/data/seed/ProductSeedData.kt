package com.example.data.seed

import com.example.data.model.Product

object ProductSeedData {
    val initialProducts: List<Product> = buildList {
        var idCounter = 1

        fun addSKU(
            brand: String,
            name: String,
            category: String,
            size: String,
            cost: Double,
            rate: Double,
            opening: Int
        ) {
            val catPrefix = when (category.lowercase()) {
                "whisky", "whiskey" -> "WHI"
                "beer" -> "BER"
                "brandy" -> "BDY"
                "rum" -> "RUM"
                "vodka" -> "VOD"
                "wine" -> "WIN"
                else -> "LIQ"
            }
            val sizeCode = size.replace(" ", "").replace("ML", "")
            val skuCode = "SKU-$catPrefix-$sizeCode-${String.format("%03d", idCounter)}"

            add(
                Product(
                    id = "p_$idCounter",
                    sku = skuCode,
                    brandName = brand,
                    name = name,
                    category = category,
                    unitSize = size,
                    currentStockLevel = opening,
                    reorderPoint = if (category.equals("Beer", ignoreCase = true)) 15 else 10,
                    costPrice = cost,
                    defaultRate = rate,
                    initialOpeningStock = opening,
                    displayOrder = idCounter,
                    isActive = true
                )
            )
            idCounter++
        }

        // 1. Brandy (18 SKUs)
        val brandyBrands = listOf(
            Triple("M.H. Brandy", "Premium Brandy", listOf("1000 ML" to 1180.0, "750 ML" to 890.0, "375 ML" to 460.0, "180 ML" to 230.0, "90 ML" to 120.0, "60 ML" to 80.0)),
            Triple("MH W/S", "Special Brandy", listOf("750 ML" to 920.0, "375 ML" to 480.0, "180 ML" to 240.0)),
            Triple("MC Brandy", "Classic Brandy", listOf("750 ML" to 780.0, "375 ML" to 400.0, "180 ML" to 200.0)),
            Triple("MCB W/S", "White Label Brandy", listOf("750 ML" to 820.0, "375 ML" to 420.0)),
            Triple("Old Admiral", "VSOP Brandy", listOf("750 ML" to 650.0, "375 ML" to 340.0, "180 ML" to 170.0)),
            Triple("Morpheus", "XO Premium Brandy", listOf("750 ML" to 1350.0))
        )
        for ((brand, name, sizes) in brandyBrands) {
            for ((size, rate) in sizes) {
                if (idCounter <= 18) {
                    addSKU(brand, name, "Brandy", size, rate * 0.82, rate, 25)
                }
            }
        }
        // Fill remaining to reach exactly 18 Brandy SKUs
        while (idCounter <= 18) {
            addSKU("OAB W/S", "Reserve Brandy", "Brandy", if (idCounter % 2 == 0) "375 ML" else "180 ML", 220.0, 270.0, 20)
        }

        // 2. Rum (8 SKUs)
        val rumBrands = listOf(
            Triple("OLD Monk", "XXXX XXX Rum", listOf("1000 ML" to 980.0, "750 ML" to 720.0, "375 ML" to 370.0, "180 ML" to 190.0, "90 ML" to 100.0)),
            Triple("M.C. Rum", "Dark Rum", listOf("750 ML" to 680.0, "375 ML" to 350.0, "180 ML" to 180.0))
        )
        for ((brand, name, sizes) in rumBrands) {
            for ((size, rate) in sizes) {
                if (idCounter <= 18 + 8) {
                    addSKU(brand, name, "Rum", size, rate * 0.80, rate, 30)
                }
            }
        }
        while (idCounter <= 26) {
            addSKU("M.C. Rum", "Spiced Rum", "Rum", "180 ML", 150.0, 190.0, 20)
        }

        // 3. Vodka (9 SKUs)
        val vodkaBrands = listOf(
            Triple("Magic Moment", "Grain Vodka", listOf("750 ML" to 850.0, "375 ML" to 440.0, "180 ML" to 220.0)),
            Triple("Romanov Vodka", "Classic Vodka", listOf("750 ML" to 620.0, "375 ML" to 320.0, "180 ML" to 160.0)),
            Triple("S.M. Vodka", "Pure Vodka", listOf("750 ML" to 790.0, "375 ML" to 400.0)),
            Triple("Oxyzen", "Crisp Vodka", listOf("750 ML" to 900.0))
        )
        for ((brand, name, sizes) in vodkaBrands) {
            for ((size, rate) in sizes) {
                if (idCounter <= 26 + 9) {
                    addSKU(brand, name, "Vodka", size, rate * 0.82, rate, 15)
                }
            }
        }
        while (idCounter <= 35) {
            addSKU("Magic Moment", "Green Apple Vodka", "Vodka", "375 ML", 380.0, 460.0, 15)
        }

        // 4. Wine (4 SKUs)
        val wineList = listOf(
            Tuple4("Ghonus Wine", "Red Wine", "750 ML", 650.0),
            Tuple4("Ghonus Wine", "White Wine", "750 ML", 650.0),
            Tuple4("Veerat Wine", "Sweet Port Wine", "750 ML", 420.0),
            Tuple4("Veerat Wine", "Premium Port Wine", "750 ML", 480.0)
        )
        for ((brand, name, size, rate) in wineList) {
            if (idCounter <= 35 + 4) {
                addSKU(brand, name, "Wine", size, rate * 0.78, rate, 18)
            }
        }

        // 5. Whisky (83 SKUs)
        val whiskyBrands = listOf(
            "Black & White", "100 Pipers", "Teachers", "Black Dog", "B.S. Pride", "RC",
            "MC Signature", "A Q Blue", "R S Whisky", "IB Whisky", "MC Whisky", "After Dark",
            "H2D Rum", "Dauty John", "D.S.P. Black", "B.P. Whisky", "8 P.M. Whisky", "O.T. Whisky",
            "O.C. Whisky", "Raja Whisky", "Haywards", "Bangalore MALT", "DK Whisky", "No.1 Whisky",
            "Raja Zin", "Bacardi +"
        )
        val whiskySizes = listOf("1000 ML", "750 ML", "375 ML", "180 ML", "90 ML", "60 ML")
        var wBrandIdx = 0
        var wSizeIdx = 0
        while (idCounter <= 39 + 83) { // 39 to 122
            val b = whiskyBrands[wBrandIdx % whiskyBrands.size]
            val s = whiskySizes[wSizeIdx % whiskySizes.size]
            val baseRate = when (s) {
                "1000 ML" -> 1650.0
                "750 ML" -> 1200.0
                "375 ML" -> 620.0
                "180 ML" -> 310.0
                "90 ML" -> 160.0
                else -> 110.0
            }
            val brandMultiplier = 1.0 + ((wBrandIdx % 5) * 0.15)
            val finalRate = (baseRate * brandMultiplier).coerceAtLeast(100.0)
            addSKU(b, "Blended Whisky", "Whisky", s, finalRate * 0.81, finalRate, 24)

            wSizeIdx++
            if (wSizeIdx % whiskySizes.size == 0) {
                wBrandIdx++
            }
        }

        // 6. Beer (31 SKUs) (Items 123 to 153)
        val beerBrands = listOf(
            "Breezer", "K.F. Ultra", "K.F. Ultra Max", "K.F. Strong", "K.F. W/S", "K.F. TIN",
            "K.F. Premium", "Knock Out", "Knock Out W/S", "Knock Out TIN", "Budweiser (M)",
            "Budweiser (M) TIN", "Budweiser (P)", "Budweiser (P) TIN", "Tuborg Strong",
            "Tuborg W/S", "Tuborg", "Carls Berg Elephant", "RC (P)", "Brocode", "RC",
            "RC W/S", "RC TIN", "Power Cool", "Carona", "Bullet"
        )
        val beerSizes = listOf("650 ML", "500 ML", "330 ML", "275 ML")
        var bBrandIdx = 0
        var bSizeIdx = 0
        while (idCounter <= 153) {
            val b = beerBrands[bBrandIdx % beerBrands.size]
            val s = if (b.contains("TIN")) "500 ML" else beerSizes[bSizeIdx % beerSizes.size]
            val rate = when (s) {
                "650 ML" -> 220.0
                "500 ML" -> 180.0
                "330 ML" -> 140.0
                else -> 120.0
            }
            addSKU(b, "Chilled Beer", "Beer", s, rate * 0.78, rate, 40)

            bSizeIdx++
            if (bSizeIdx % beerSizes.size == 0) {
                bBrandIdx++
            }
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}

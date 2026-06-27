package com.example.data

data class MonthRegression(
    val month: Int,
    val a: Double,
    val b: Double,
    val c: Double,
    val r: Double,
    val formulaText: String,
    val maxX: Double,
    val maxY: Double,
    val xLabelStep: Double = 5.0,
    val yLabelStep: Double = 1.0
)

object RegressionConstants {
    val MONTHS = listOf(
        MonthRegression(1, 0.0086, -0.0014, 1.0178, 0.88, "y = 0.0086x² - 0.0014x + 1.0178", 25.0, 6.0, 5.0, 1.0),
        MonthRegression(2, 0.0007, 0.1749, 0.1365, 0.91, "y = 0.0007x² + 0.1749x + 0.1365", 20.0, 5.0, 5.0, 1.0),
        MonthRegression(3, 0.0025, 0.1506, 0.0206, 0.92, "y = 0.0025x² + 0.1506x + 0.0206", 20.0, 5.0, 5.0, 1.0),
        MonthRegression(4, 0.0131, -0.0491, 0.6404, 0.73, "y = 0.0131x² - 0.0491x + 0.6404", 20.0, 3.5, 5.0, 0.5),
        MonthRegression(5, 0.0141, -0.0394, 0.5811, 0.79, "y = 0.0141x² - 0.0394x + 0.5811", 20.0, 4.0, 5.0, 1.0),
        MonthRegression(6, 0.0153, -0.0905, 0.5601, 0.59, "y = 0.0153x² - 0.0905x + 0.5601", 15.0, 3.0, 5.0, 0.5),
        MonthRegression(7, 0.0143, -0.0755, 0.6145, 0.72, "y = 0.0143x² - 0.0755x + 0.6145", 25.0, 6.0, 5.0, 1.0),
        MonthRegression(8, 0.0105, -0.0518, 0.4692, 0.48, "y = 0.0105x² - 0.0518x + 0.4692", 20.0, 3.0, 5.0, 0.5),
        MonthRegression(9, 0.0169, -0.0655, 0.7823, 0.54, "y = 0.0169x² - 0.0655x + 0.7823", 20.0, 4.5, 5.0, 0.5),
        MonthRegression(10, 0.0023, 0.1378, 0.4406, 0.91, "y = 0.0023x² + 0.1378x + 0.4406", 25.0, 7.0, 5.0, 1.0),
        MonthRegression(11, 0.0038, 0.0886, 0.7987, 0.88, "y = 0.0038x² + 0.0886x + 0.7987", 25.0, 5.0, 5.0, 1.0),
        MonthRegression(12, 0.0037, 0.0875, 0.659, 0.88, "y = 0.0037x² + 0.0875x + 0.659", 25.0, 5.0, 5.0, 1.0)
    )

    fun getForMonth(month: Int): MonthRegression {
        return MONTHS.firstOrNull { it.month == month } ?: MONTHS[0]
    }
}

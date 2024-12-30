package com.example.thirdlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt


// вхідні дані
data class Data(
    val power: Double = 0.0,
    val electricity: Double = 0.0,
    val deviation1: Double = 0.0,
    val deviation2: Double = 0.0,
)

// результати розрахунків
data class CalculationResults(
    val profitBefore: Double = 0.0,
    val profitAfter: Double = 0.0,
)

@Composable
fun CalculatorScreen(
) {
    var data by remember { mutableStateOf(Data()) }
    var results by remember { mutableStateOf<CalculationResults?>(null) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Калькулятор прибутку від сонячних електростанцій",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        InputField("Середньодобова потужність, МВт", data.power) { data = data.copy(power = it) }
        InputField("Середньоквадратичне відхилення, МВт", data.deviation1) {
            data = data.copy(deviation1 = it)
        }
        InputField("Середньоквадратичне відхилення 2, МВт", data.deviation2) {
            data = data.copy(deviation2 = it)
        }
        InputField("Вартість електроенергії, МВт", data.electricity) {
            data = data.copy(electricity = it)
        }

        Button(
            onClick = { results = calculateResults(data) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .size(width = 300.dp, height = 50.dp),

            ) {
            Text("Розрахувати")
        }

        results?.let { DisplayResults(it) }
    }
}

@Composable
fun InputField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit
) {
    OutlinedTextField(
        value = if (value == 0.0) "" else value.toString(),
        onValueChange = {
            onValueChange(it.toDoubleOrNull() ?: 0.0)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun DisplayResults(results: CalculationResults) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text(
            "Результати розрахунків:",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ResultSection("Прибутки:") {
            ResultItem("до вдосконалення", results.profitBefore, "тис.грн")
            ResultItem("після вдосконалення", results.profitAfter, "тис.грн")
        }

    }
}

@Composable
fun ResultSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
    content()
}

@Composable
fun ResultItem(label: String, value: Double, sign: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(String.format("%.2f", value) + " " + sign)
    }
}

// функція нормального закоону розподілу потужності (формула 9.1)
private fun normalDistribution(x: Double, power: Double, sigma: Double): Double {
    return (1 / (sigma * sqrt(2 * PI))) *
            exp(-(x - power).pow(2) / (2 * sigma.pow(2)))
}

// інтегрування
private fun integrate(
    a: Double, // нижня межа
    b: Double, // верхня межа
    n: Int = 100000, // кількість точок для інтегрування
    power: Double,
    sigma: Double
): Double {
    val h = (b - a) / n
    var sum = (normalDistribution(a, power, sigma) +
            normalDistribution(b, power, sigma)) / 2

    for (i in 1 until n) {
        val x = a + i * h
        sum += normalDistribution(x, power, sigma)
    }

    return h * sum
}

private fun calculateEnergyWithoutImbalance(
    power: Double,
    sigma: Double,
    lowerBound: Double,
    upperBound: Double,
): Double {
    return integrate(
        a = lowerBound,
        b = upperBound,
        power = power,
        sigma = sigma
    ) * 100 // переводимо у відсотки
}

private fun calculateResults(data: Data): CalculationResults {
    // діапазони
    val lowerBound = 4.75
    val upperBound = 5.25

    // розрахунок частки енергії без небалансів до покращення (δW1)
    val energyWithoutImbalance1 = calculateEnergyWithoutImbalance(
        data.power,
        data.deviation1,
        lowerBound,
        upperBound
    ).roundToInt().toDouble()

    // розрахунок частки енергії без небалансів після покращення (δW2)
    val energyWithoutImbalance2 = calculateEnergyWithoutImbalance(
        data.power,
        data.deviation2,
        lowerBound,
        upperBound
    ).roundToInt().toDouble()

    // енергія W1
    val energy1 = data.power * 24 * energyWithoutImbalance1 / 100

    // прибуток П1
    val profit1 = energy1 * data.electricity

    // енергія W2
    val energy2 = data.power * 24 * (1 - energyWithoutImbalance1 / 100)

    // штраф Ш1
    val fine1 = energy2 * data.electricity

    // загальний прибуток перед покращенням
    val profitBefore = profit1 - fine1

    // енергія W3
    val energy3 = data.power * 24 * energyWithoutImbalance2 / 100

    // прибуток П2
    val profit2 = energy3 * data.electricity

    // енергія W4
    val energy4 = data.power * 24 * (1 - energyWithoutImbalance2 / 100)

    // штраф Ш2
    val fine2 = energy4 * data.electricity

    // загальний прибуток після покращення
    val profitAfter = profit2 - fine2

    return CalculationResults(
        profitBefore = profitBefore,
        profitAfter = profitAfter
    )
}

//private fun calculateResults(data: Data): CalculationResults {
//    // діапазони
//    val lowerBound = 4.75
//    val upperBound = 5.25
//
//    // діапазони
//    val energyWithoutImbalance1 = 0.20
//
//    // Calculate energy W1 (24 МВт·год)
//    val energy1 = data.powerC * 24 * energyWithoutImbalance1
//
//    // Calculate profit П1 (168 тис. грн)
//    val profit1 = energy1 * data.electricityPrice
//
//    // Calculate energy W2 (96 МВт·год)
//    val energy2 = data.powerC * 24 * (1 - energyWithoutImbalance1)
//
//    // Calculate fine Ш1 (672 тис. грн)
//    val fine1 = energy2 * data.electricityPrice
//
//    // Calculate total profit before improvement (-504 тис. грн)
//    val profitBefore = profit1 - fine1
//
//    // After improvement calculations
//    val energyWithoutImbalance2 = 0.68 // 68% from formula (9.7)
//
//    // Calculate energy W3 (81.6 МВт·год)
//    val energy3 = data.powerC * 24 * energyWithoutImbalance2
//
//    // Calculate profit П2 (571.2 тис. грн)
//    val profit2 = energy3 * data.electricityPrice
//
//    // Calculate energy W4 (38.4 МВт·год)
//    val energy4 = data.powerC * 24 * (1 - energyWithoutImbalance2)
//
//    // Calculate fine Ш2 (268.8 тис. грн)
//    val fine2 = energy4 * data.electricityPrice
//
//    // Calculate total profit after improvement (302.4 тис. грн)
//    val profitAfter = profit2 - fine2
//
//    return CalculationResults(
//        profitBefore = profitBefore,
//        profitAfter = profitAfter,
//    )
//}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorScreen()
        }
    }
}

package com.example.kotlincalculator

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private lateinit var calculationText: TextView
    private lateinit var resultText: TextView

    private val expression = StringBuilder()
    private val numberFormatter = DecimalFormat("#.##########")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        calculationText = findViewById(R.id.calculationText)
        resultText = findViewById(R.id.resultText)

        bindCalculatorButtons()
        updateDisplay()
    }

    private fun bindCalculatorButtons() {
        val numberButtons = mapOf(
            R.id.zeroButton to "0",
            R.id.oneButton to "1",
            R.id.twoButton to "2",
            R.id.threeButton to "3",
            R.id.fourButton to "4",
            R.id.fiveButton to "5",
            R.id.sixButton to "6",
            R.id.sevenButton to "7",
            R.id.eightButton to "8",
            R.id.nineButton to "9",
        )

        numberButtons.forEach { (buttonId, value) ->
            findViewById<Button>(buttonId).setOnClickListener {
                appendNumber(value)
            }
        }

        findViewById<Button>(R.id.decimalButton).setOnClickListener { appendDecimal() }
        findViewById<Button>(R.id.addButton).setOnClickListener { appendOperator("+") }
        findViewById<Button>(R.id.subtractButton).setOnClickListener { appendOperator("-") }
        findViewById<Button>(R.id.multiplyButton).setOnClickListener { appendOperator("×") }
        findViewById<Button>(R.id.divideButton).setOnClickListener { appendOperator("÷") }
        findViewById<Button>(R.id.percentButton).setOnClickListener { applyPercent() }
        findViewById<Button>(R.id.clearButton).setOnClickListener { clearCalculator() }
        findViewById<Button>(R.id.deleteButton).setOnClickListener { deleteLastCharacter() }
        findViewById<Button>(R.id.equalsButton).setOnClickListener { calculateResult() }
    }

    private fun appendNumber(number: String) {
        val currentNumber = expression.currentNumber()
        if (number == "0" && currentNumber == "0") return
        if (currentNumber == "0" && !currentNumber.contains(".")) {
            expression.deleteCharAt(expression.lastIndex)
        }

        expression.append(number)
        updateDisplay()
    }

    private fun appendDecimal() {
        val currentNumber = expression.currentNumber()
        if (currentNumber.contains(".")) return

        if (expression.isEmpty() || expression.last().isOperator()) {
            expression.append("0")
        }

        expression.append(".")
        updateDisplay()
    }

    private fun appendOperator(operator: String) {
        if (expression.isEmpty()) {
            if (operator == "-") {
                expression.append(operator)
                updateDisplay()
            }
            return
        }

        val lastCharacter = expression.last()
        if (lastCharacter.isOperator()) {
            expression.setCharAt(expression.lastIndex, operator.single())
        } else {
            expression.append(operator)
        }

        updateDisplay()
    }

    private fun applyPercent() {
        if (expression.isEmpty() || expression.last().isOperator()) return

        val result = evaluateExpression(expression.toString()) / 100
        replaceWithResult(result)
    }

    private fun clearCalculator() {
        expression.clear()
        updateDisplay()
    }

    private fun deleteLastCharacter() {
        if (expression.isNotEmpty()) {
            expression.deleteCharAt(expression.lastIndex)
            updateDisplay()
        }
    }

    private fun calculateResult() {
        if (expression.isEmpty() || expression.last().isOperator()) return

        runCatching {
            evaluateExpression(expression.toString())
        }.onSuccess { result ->
            replaceWithResult(result)
        }.onFailure {
            resultText.text = getString(R.string.calculator_error)
        }
    }

    private fun replaceWithResult(result: Double) {
        val formattedResult = formatResult(result)
        expression.clear()
        expression.append(formattedResult)
        updateDisplay()
    }

    private fun updateDisplay() {
        calculationText.text = expression.toString()
        resultText.text = expression.toString().ifEmpty { "0" }
    }

    private fun evaluateExpression(input: String): Double {
        val values = ArrayDeque<Double>()
        val operators = ArrayDeque<Char>()
        var index = 0

        while (index < input.length) {
            val character = input[index]
            if (character.isDigit() || character == '.' || character == '-' && input.isUnaryMinus(index)) {
                val startIndex = index
                index++
                while (index < input.length && (input[index].isDigit() || input[index] == '.')) {
                    index++
                }
                values.addLast(input.substring(startIndex, index).toDouble())
                continue
            }

            if (character.isOperator()) {
                while (operators.isNotEmpty() && operators.last().precedence() >= character.precedence()) {
                    values.addLast(applyOperator(values.removeLast(), values.removeLast(), operators.removeLast()))
                }
                operators.addLast(character)
            }

            index++
        }

        while (operators.isNotEmpty()) {
            values.addLast(applyOperator(values.removeLast(), values.removeLast(), operators.removeLast()))
        }

        return values.last()
    }

    private fun applyOperator(right: Double, left: Double, operator: Char): Double {
        return when (operator) {
            '+' -> left + right
            '-' -> left - right
            '×' -> left * right
            '÷' -> {
                require(right != 0.0) { "Cannot divide by zero" }
                left / right
            }
            else -> error("Unsupported operator")
        }
    }

    private fun formatResult(result: Double): String {
        return if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            numberFormatter.format(result)
        }
    }

    private fun Char.isOperator(): Boolean = this in listOf('+', '-', '×', '÷')

    private fun Char.precedence(): Int {
        return when (this) {
            '+', '-' -> 1
            '×', '÷' -> 2
            else -> 0
        }
    }

    private fun String.isUnaryMinus(index: Int): Boolean {
        return this[index] == '-' && (index == 0 || this[index - 1].isOperator())
    }

    private fun StringBuilder.currentNumber(): String {
        val lastOperatorIndex = indexOfLast { it.isOperator() }
        return substring(lastOperatorIndex + 1)
    }
}

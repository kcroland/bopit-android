package com.example.bopitandroid

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bopitandroid.databinding.FragmentSecondBinding
import com.example.bopitandroid.network.MyTrivia
import com.example.bopitandroid.network.ServiceBuilder
import com.example.bopitandroid.network.TriviaService
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.abs

const val SECOND_IN_NANO = 1000000000
const val DIFFICULTY = 3
const val ACCELERATION_TEST = 10

const val BOP = "bop it!"
const val TWIST = "twist it!"
const val PULL = "pull it!"
const val FREEZE = "FREEZE!"
const val HIDE = "hide it!"
const val TRIVIA = "TRIVIA!"

val GESTURES_IMAGES: Map<String, Int> = mapOf(
    BOP to R.drawable.bop_arrow,
    TWIST to R.drawable.twist_arrow,
    PULL to R.drawable.pull_arrow,
    FREEZE to R.drawable.freeze,
    HIDE to R.drawable.hide_it_icon,
)

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var currentWait: Job

    // helper variables for game logic
    private var score: Int = 0
    private var initialTimeStamp: Double = 0.0
    private var currentGesture: String = ""
    private var sensorIsLive = false

    // helper variables for specific gestures
    private var questionAnswer = false
    private var didAnswerQuestion = false
    private var noMovement = true

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        // trivia button behavior
        binding.trueButton.setOnClickListener { checkAnswer(true) }
        binding.trueButton.isClickable = false
        binding.falseButton.setOnClickListener { checkAnswer(false) }
        binding.falseButton.isClickable = false

        return binding.root

    }

    override fun onStart() {
        super.onStart()

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.countdownText.text = "" + (millisUntilFinished / 1000 + 1)
            }

            override fun onFinish() {
                getNextGesture()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
        _binding = null
    }

    /* Functions below for game setup, gesture logic, and game end */

    private fun getNextGesture() {
        resetGameView()

        var newGesture = GESTURES_IMAGES.keys.random()
        if (score != 0 && score % 5 == 0) {
            newGesture = TRIVIA
            showTrivia()
        } else {
            while (newGesture == currentGesture) {
                newGesture = GESTURES_IMAGES.keys.random()
            }
            if (newGesture == FREEZE) binding.gestureText.setTextColor(Color.parseColor("#00C2FF"))
            binding.gestureImage.setImageResource(GESTURES_IMAGES[newGesture]!!)
        }
        binding.gestureText.text = newGesture

        currentGesture = newGesture
        if (sensorIsLive) stopGestureSensor()
        if (currentGesture != TRIVIA) startGestureSensor()
    }

    // Return the game view to the basic state following the initial countdown
    private fun resetGameView() {
        // Reset helper variables
        initialTimeStamp = 0.0
        questionAnswer = false
        didAnswerQuestion = false
        noMovement = true

        // Show gesture image and name; Set original color
        binding.countdownText.visibility = View.INVISIBLE
        binding.gestureText.visibility = View.VISIBLE
        binding.gestureText.setTextColor(Color.parseColor("#FCFF64"))
        binding.gestureImage.visibility = View.VISIBLE

        // Hide and disable all trivia gesture views
        binding.questionText.visibility = View.INVISIBLE
        binding.trueButton.visibility = View.INVISIBLE
        binding.falseButton.visibility = View.INVISIBLE
        binding.trueButton.isClickable = false
        binding.falseButton.isClickable = false

        // Reorient gesture image
        binding.gestureImage.apply {
            rotationX = 0.0F
            rotationY = 0.0F
            rotation = 0.0F
            translationX = 0.0F
            translationY = 0.0F
        }
    }

    private fun startGestureSensor() {
        val sensorType = when (currentGesture) {
            HIDE -> Sensor.TYPE_PROXIMITY
            else -> Sensor.TYPE_ACCELEROMETER
        }

        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(sensorType)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }

        sensorIsLive = true

        if (sensorType == Sensor.TYPE_PROXIMITY) {
            currentWait = GlobalScope.launch(Dispatchers.Main) {
                delay(3000)
                gameOver()
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (initialTimeStamp == 0.0) initialTimeStamp = event.timestamp.toDouble()
        val elapsedTime = event.timestamp.toDouble() / SECOND_IN_NANO -
                initialTimeStamp / SECOND_IN_NANO

        if (event.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            checkAccelerometerGesture(event, elapsedTime)
        } else {
            checkProximityGesture(event)
        }
    }

    private fun checkProximityGesture(event: SensorEvent) {
        val isHidden = event.values[0] < 3
        if (isHidden) {
            currentWait.cancel()
            onSuccessfulGesture()
        }
    }

    private fun checkAccelerometerGesture(event: SensorEvent, elapsedTime: Double) {
        if (elapsedTime > DIFFICULTY && (currentGesture != FREEZE) ||
            (currentGesture == FREEZE && !noMovement)) {
            gameOver()
        }

        val sides: Float = event.values[0]
        val upDown: Float = event.values[1]

        // Allow for gesture image rotation based on sensor values
        binding.gestureImage.apply {
            rotationX = upDown * 3f
            rotationY = sides * 3f
            rotation = -sides
            translationX = sides * -10
            translationY = upDown * 10
        }

        if (abs(sides) > 5 && abs(upDown) > 5) { noMovement = false }

        val wasPulled = currentGesture == PULL && upDown > ACCELERATION_TEST
        val wasTwisted = currentGesture == TWIST && (abs(sides) > ACCELERATION_TEST)
        val wasBopped = currentGesture == BOP && upDown < -ACCELERATION_TEST
        val wasFroze = currentGesture == FREEZE && elapsedTime > DIFFICULTY && noMovement

        if (wasPulled || wasTwisted || wasBopped || wasFroze) onSuccessfulGesture()
    }

    private fun onSuccessfulGesture() {
        binding.gestureText.setTextColor(Color.parseColor("#64FF8F"))
        if (sensorIsLive) stopGestureSensor()
        score += 1

        object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                return
            }

            override fun onFinish() {
                getNextGesture()
            }
        }.start()
    }

    private fun showTrivia() {
        binding.gestureImage.visibility = View.INVISIBLE
        binding.gestureText.setTextColor(Color.parseColor("#FFB864"))

        loadTrivia()

        binding.trueButton.visibility = View.VISIBLE
        binding.trueButton.isClickable = true
        binding.falseButton.visibility = View.VISIBLE
        binding.falseButton.isClickable = true
        binding.questionText.visibility = View.VISIBLE
    }

    private fun loadTrivia() {
        val destinationService  = ServiceBuilder.buildService(TriviaService::class.java)
        val requestCall = destinationService.getQuestions()

        requestCall.enqueue(object : Callback<MyTrivia?> {
            override fun onResponse(call: Call<MyTrivia?>, response: Response<MyTrivia?>) {
                val triviaQuestion = response.body()!!.results[0]
                binding.questionText.text = triviaQuestion.question
                questionAnswer = triviaQuestion.correct_answer.toBoolean()

                currentWait = GlobalScope.launch(Dispatchers.Main) {
                    delay(10000)
                    gameOver()
                }
            }

            override fun onFailure(call: Call<MyTrivia?>, t: Throwable) {
                Log.d("Response", "Failure : ${t}")
            }
        })
    }

    private fun checkAnswer(answerSelected: Boolean) {
        didAnswerQuestion = true
        binding.trueButton.isClickable = false
        binding.falseButton.isClickable = false
        currentWait.cancel()

        if (questionAnswer == answerSelected) {
            onSuccessfulGesture()
        } else {
            gameOver()
        }
    }

    private fun gameOver() {
        binding.gestureText.setTextColor(Color.parseColor("#EB3743"))
        if (sensorIsLive) stopGestureSensor()

        object : CountDownTimer(1500, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                return
            }

            override fun onFinish() {
                val action = SecondFragmentDirections.actionSecondFragmentToGameOver(score)
                findNavController().navigate(action)
            }
        }.start()
    }

    private fun stopGestureSensor() { sensorManager.unregisterListener(this) }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}
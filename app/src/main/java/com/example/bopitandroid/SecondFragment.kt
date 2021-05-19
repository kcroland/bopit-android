package com.example.bopitandroid

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bopitandroid.databinding.FragmentSecondBinding
import kotlin.math.abs

const val SECOND_IN_NANO = 1000000000

const val BOP = "bop it!"
const val TWIST = "twist it!"
const val PULL = "pull it!"
const val FREEZE = "FREEZE!"

val GESTURES_IMAGES: Map<String, Int> = mapOf(
    BOP to R.drawable.bop_arrow,
    TWIST to R.drawable.twist_arrow,
    PULL to R.drawable.pull_arrow,
    FREEZE to R.drawable.freeze
)

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    private var noMovement = true
    private var score: Int = 0
    private var initialTimeStamp: Double = 0.0
    private  var currentGesture: String = ""

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onStart() {
        super.onStart()

        object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.countdownText.text = "" + (millisUntilFinished / 1000 + 1)
            }

            override fun onFinish() {
                binding.countdownText.text = "Go!"

                startGame()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        sensorManager.unregisterListener(this)
        _binding = null
    }

    private fun startGame() {
        binding.countdownText.visibility = View.INVISIBLE
        getNextGesture()
        if (currentGesture == FREEZE) binding.gestureText.setTextColor(Color.parseColor("#00C2FF"))
        binding.gestureText.visibility = View.VISIBLE
        binding.gestureImage.visibility = View.VISIBLE
        startGestureSensor()
    }

    private fun startGestureSensor() {
        sensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    private fun stopGestureSensor() {
        sensorManager.unregisterListener(this)
    }

    private fun getNextGesture() {
        var newGesture = GESTURES_IMAGES.keys.random()
        while (newGesture == currentGesture) {
            newGesture = GESTURES_IMAGES.keys.random()
        }
        currentGesture = newGesture

        binding.gestureText.text = newGesture
        binding.gestureImage.setImageResource(GESTURES_IMAGES[newGesture]!!)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            if (initialTimeStamp == 0.0) initialTimeStamp = event.timestamp.toDouble()

            val elapsedTime = event.timestamp.toDouble() / SECOND_IN_NANO -
                    initialTimeStamp.toDouble() / SECOND_IN_NANO

            if (elapsedTime > 3 && (currentGesture != FREEZE) || (currentGesture == FREEZE && !noMovement)) {
                binding.gestureText.setTextColor(Color.parseColor("#EB3743"))
                stopGestureSensor()
                gameOver()
            }

            val sides = event.values[0]
            val upDown = event.values[1]

            binding.gestureImage.apply {
                rotationX = upDown * 3f
                rotationY = sides * 3f
                rotation = -sides
                translationX = sides * -10
                translationY = upDown *10
            }

//            binding.gestureText.text = "upDown = ${upDown.toInt()}\n leftRight ${sides.toInt()}\n z: ${event.values[2].toInt()}"
//            binding.gestureText.textSize = 24.0F

            if (abs(sides) > 5 && abs(upDown) > 5) noMovement = false

            val wasTwisted = currentGesture == TWIST && (abs(sides) > 10)
            val wasBopped = currentGesture == BOP && upDown < -10
            val wasFroze = currentGesture == FREEZE && elapsedTime > 3 && noMovement

            if (wasTwisted || wasBopped || wasFroze) {
                stopGestureSensor()
                onSuccessfulGesture()
            }
        }
    }

    private fun onSuccessfulGesture() {
        noMovement = true
        score += 1
        binding.gestureText.setTextColor(Color.parseColor("#59FF74"))
        object : CountDownTimer(2000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                return
            }

            override fun onFinish() {
                initialTimeStamp = 0.0
                binding.gestureText.setTextColor(Color.parseColor("#FCFF64"))
                getNextGesture()
                startGestureSensor()
            }
        }.start()
    }

    private fun gameOver() {
        val action = SecondFragmentDirections.actionSecondFragmentToGameOver(score)
        findNavController().navigate(action)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }
}
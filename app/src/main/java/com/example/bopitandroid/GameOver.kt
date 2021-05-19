package com.example.bopitandroid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bopitandroid.databinding.FragmentGameOverBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [GameOver.newInstance] factory method to
 * create an instance of this fragment.
 */
class GameOver : Fragment() {

    companion object {
        const val FINAL_SCORE = "finalScore"
    }

    private var _binding: FragmentGameOverBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentGameOverBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args: GameOverArgs by navArgs()
        binding.scoreText.text = args.finalScore.toString()
//        arguments?.let {
//            binding.scoreText.text = it.getString(FINAL_SCORE).toString()
//        }

        binding.playAgainButton.setOnClickListener {
            findNavController().navigate(R.id.action_gameOver_to_SecondFragment)
        }
        binding.returnButton.setOnClickListener {
            findNavController().navigate(R.id.action_gameOver_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
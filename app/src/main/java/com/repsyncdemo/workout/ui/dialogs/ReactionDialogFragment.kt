package com.repsyncdemo.workout.ui.dialogs

/**
 * File overview: Presents a focused dialog or picker flow and returns the selected data to the calling screen.
 */

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.repsyncdemo.workout.databinding.DialogReactionsBinding
import com.repsyncdemo.workout.viewmodel.FeedViewModel

class ReactionDialogFragment : DialogFragment() {

    private var _binding: DialogReactionsBinding? = null
    private val binding get() = _binding!!

    private val feedViewModel: FeedViewModel by activityViewModels()


    companion object {
        private const val ARG_POST_ID = "post_id"
        private const val ARG_X = "x"
        private const val ARG_Y = "y"

        fun newInstance(postId: String, anchorView: View? = null): ReactionDialogFragment {
            val fragment = ReactionDialogFragment()
            val args = Bundle()
            args.putString(ARG_POST_ID, postId)
            
            anchorView?.let {
                val location = IntArray(2)
                it.getLocationOnScreen(location)
                args.putInt(ARG_X, location[0])
                args.putInt(ARG_Y, location[1])
            }
            
            fragment.arguments = args
            return fragment
        }
    }

    // Sets up this screen.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, 0)
        isCancelable = true
    }

    // Sets up this screen.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = DialogReactionsBinding.inflate(inflater, container, false)

        val postID2 = arguments?.getString(ARG_POST_ID)

        fun handleReactionClick(emoji: String) {
            feedViewModel.toggleReaction(postID2, emoji)

            dismiss()
        }

        //When user clicks emoji, add emoji and userID to FeedViewModel adding also to firebase
        binding.emojiFire.setOnClickListener {view -> handleReactionClick("🔥") }

        binding.emojiSquat.setOnClickListener {view -> handleReactionClick("🏋️‍♂️")  }

        binding.emojiTrophy.setOnClickListener {view -> handleReactionClick("🏆")  }

        binding.emojiArm.setOnClickListener {view -> handleReactionClick("💪️")  }

        binding.emojiMedal.setOnClickListener {view -> handleReactionClick("🥇")  }


        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.setCanceledOnTouchOutside(true)
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            val x = arguments?.getInt(ARG_X, -1) ?: -1
            val y = arguments?.getInt(ARG_Y, -1) ?: -1
            
            if (x != -1 && y != -1) {
                setGravity(android.view.Gravity.TOP or android.view.Gravity.START)
                val params = attributes
                params.x = x
                // Position slightly above the button
                params.y = y - 150 
                attributes = params
            }

            setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    // Clears the view binding.
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
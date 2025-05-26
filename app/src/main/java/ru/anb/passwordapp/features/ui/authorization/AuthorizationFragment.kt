package ru.anb.passwordapp.features.ui.authorization

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint
import ru.anb.passwordapp.R
import ru.anb.passwordapp.core.ui.BaseFragment
import ru.anb.passwordapp.core.ui.MainActivity
import ru.anb.passwordapp.data.AuthResult
import ru.anb.passwordapp.databinding.FragmentAuthorizationBinding
import ru.anb.passwordapp.features.ui.home.HomeActivity
import ru.anb.passwordapp.features.ui.home.SetupActivity

@AndroidEntryPoint
class AuthorizationFragment : BaseFragment<FragmentAuthorizationBinding>() {
    override val bindingInflater: (LayoutInflater, ViewGroup?) -> FragmentAuthorizationBinding =
        { inflater, container ->
            FragmentAuthorizationBinding.inflate(inflater, container, false)
        }

    private val viewModel: AuthorizationViewModel by viewModels()
    private lateinit var mAuth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
                val intent = Intent(requireContext(), HomeActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
                return
            }


        val inputList = listOf(
            binding.authMail,
            binding.authPassword
        )

        viewModel.authState.observe(viewLifecycleOwner) {
            when (it) {
                AuthResult.Loading -> binding.progressBar.visibility = View.VISIBLE
                is AuthResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), it.e.message.toString(), Toast.LENGTH_LONG)
                        .show()
                }

                is AuthResult.Success -> {

                    val user = FirebaseAuth.getInstance().currentUser

                    user?.let {
                        if (it.metadata?.creationTimestamp == it.metadata?.lastSignInTimestamp) {
                            val intent = Intent(requireContext(), SetupActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        } else {
                            val intent = Intent(requireContext(), HomeActivity::class.java)
                            startActivity(intent)
                            requireActivity().finish()
                        }
                    }
                }
            }
        }


        binding.signIn.setOnClickListener {
            val allValidation = inputList.map { it.isValid() }

            if (allValidation.all { it }) {
                viewModel.sendCredentials(
                    email = binding.authMail.text(),
                    password = binding.authPassword.text()
                )
            }
        }
        binding.navigateToSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_authorizationFragment_to_registrationFragment)
        }

    }
}
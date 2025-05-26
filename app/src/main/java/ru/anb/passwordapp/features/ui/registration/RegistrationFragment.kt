package ru.anb.passwordapp.features.ui.registration

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.anb.passwordapp.R
import ru.anb.passwordapp.core.ui.BaseFragment
import ru.anb.passwordapp.data.AuthResult
import ru.anb.passwordapp.databinding.FragmentRegistrationBinding
import ru.anb.passwordapp.features.ui.home.SetupActivity

@AndroidEntryPoint
class RegistrationFragment : BaseFragment<FragmentRegistrationBinding>() {
    override val bindingInflater: (LayoutInflater, ViewGroup?) -> FragmentRegistrationBinding =
        { inflater, container ->
            FragmentRegistrationBinding.inflate(inflater, container, false)

        }
    private val viewModel: RegistrationViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inputList = listOf(
            binding.signUpEmail,
            binding.signUpPasswordLayout
        )

        viewModel.authState.observe(viewLifecycleOwner) {
            when (it) {
                AuthResult.Loading -> binding.progressBarRegistration.visibility = View.VISIBLE
                is AuthResult.Error -> {
                    binding.progressBarRegistration.visibility = View.GONE
                    Toast.makeText(requireContext(), it.e.message.toString(), Toast.LENGTH_LONG)
                        .show()
                }

                is AuthResult.Success -> {
                    val intent = Intent(requireContext(), SetupActivity::class.java) // Используем requireContext() для получения контекста
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }

        binding.startSignUp.setOnClickListener {
            val allValidation = inputList.map { it.isValid() }
            if (allValidation.all { it }) {
                viewModel.sendCredentials(
                    email = binding.signUpEmail.text(),
                    password = binding.signUpPasswordLayout.text()
                )
            }
        }
    }
}
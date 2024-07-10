package com.example.progettoprogrammazionemobile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.progettoprogrammazionemobile.databinding.FragmentProfileBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val viewModel: DataViewModel by viewModels()
    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadUserProfile()

        binding.Bsave.setOnClickListener {
            saveUserProfile()
        }

        val signOutButton: Button = binding.root.findViewById(R.id.sign_out)
        signOutButton.setOnClickListener {
            AuthUI.getInstance()
                .signOut(requireContext())
                .addOnCompleteListener {
                    val intent = Intent(requireContext(), WelcomeActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
        }

        return binding.root
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            binding.Temail.text = user.email
            binding.Tname.text = user.displayName?.split(" ")?.firstOrNull() ?: ""
            binding.Tsurname.text = user.displayName?.split(" ")?.lastOrNull() ?: ""

            firestore.collection(user.uid).document("Account").get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        binding.Tphone.setText(document.getString("phone"))

                        val fixedIncomeField = document.get("fixed_income")
                        val fixedExpensesField = document.get("fixed_expenses")

                        val fixedIncome = fixedIncomeField?.toString()?.toDoubleOrNull() ?: 0.0
                        val fixedExpenses = fixedExpensesField?.toString()?.toDoubleOrNull() ?: 0.0

                        viewModel.setFixedEntries(fixedIncome.toString())
                        viewModel.setFixedOut(fixedExpenses.toString())

                        binding.TfixedIn.setText(fixedIncome.toString())
                        binding.TfixedOut.setText(fixedExpenses.toString())
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            val phone = binding.Tphone.text.toString()
            val fixedExpenses = binding.TfixedOut.text.toString().toDoubleOrNull() ?: 0.0
            val fixedIncome = binding.TfixedIn.text.toString().toDoubleOrNull() ?: 0.0

            val userData = hashMapOf(
                "phone" to phone,
                "fixed_expenses" to fixedExpenses,
                "fixed_income" to fixedIncome
            )

            firestore.collection(user.uid).document("Account").set(userData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show()
                    viewModel.setFixedEntries(fixedIncome.toString())
                    viewModel.setFixedOut(fixedExpenses.toString())
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Saving Failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

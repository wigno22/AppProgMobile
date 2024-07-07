// ProfileFragment.kt
package com.example.progettoprogrammazionemobile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var Name: TextView
    private lateinit var Surname: TextView
    private lateinit var Email: TextView
    private lateinit var Phone: EditText
    private lateinit var Spesefisse: EditText
    private lateinit var Entratefisse: EditText
    private lateinit var Save: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val viewModel: DataViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        Name = view.findViewById(R.id.Tname)
        Surname = view.findViewById(R.id.Tsurname)
        Email = view.findViewById(R.id.Temail)
        Phone = view.findViewById(R.id.Tphone)
        Spesefisse = view.findViewById(R.id.TfixedOut)
        Entratefisse = view.findViewById(R.id.TfixedIn)
        Save = view.findViewById(R.id.Bsave)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadUserProfile()

        Save.setOnClickListener {
            saveUserProfile()
        }

        val signOutButton: Button = view.findViewById(R.id.sign_out)
        signOutButton.setOnClickListener {
            AuthUI.getInstance()
                .signOut(requireContext())
                .addOnCompleteListener {
                    val intent = Intent(requireContext(), WelcomeActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
        }

        return view
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            Email.text = user.email
            Name.text = user.displayName?.split(" ")?.firstOrNull() ?: ""
            Surname.text = user.displayName?.split(" ")?.lastOrNull() ?: ""

            firestore.collection(user.uid).document("Account").get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Phone.setText(document.getString("phone"))

                        val fixedExpenses = document.getDouble("fixed_expenses") ?: 0.0
                        val fixedIncome = document.getDouble("fixed_income") ?: 0.0

                        Spesefisse.setText(fixedExpenses.toString())
                        Entratefisse.setText(fixedIncome.toString())

                        viewModel.setFixedEntries(fixedIncome)
                        viewModel.setFixedOut(fixedExpenses)
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle the error
                }
        }
    }

    private fun saveUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            val fixedExpenses = Spesefisse.text.toString().toDoubleOrNull() ?: 0.0
            val fixedIncome = Entratefisse.text.toString().toDoubleOrNull() ?: 0.0

            val userData = hashMapOf(
                "phone" to Phone.text.toString(),
                "fixed_expenses" to fixedExpenses,
                "fixed_income" to fixedIncome
            )

            firestore.collection(user.uid).document("Account").update(userData as Map<String, Any>)
                .addOnSuccessListener {
                    // Handle success
                    Toast.makeText(context, "Saved Successfully", Toast.LENGTH_SHORT).show()
                    viewModel.setFixedEntries(fixedIncome)
                    viewModel.setFixedOut(fixedExpenses)
                }
                .addOnFailureListener { e ->
                    // Handle the error
                    Toast.makeText(context, "Saving Failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
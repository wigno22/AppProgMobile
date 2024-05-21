package com.example.progettoprogrammazionemobile

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_account, container, false)
        // Inflate the layout for this fragment
        val textName: TextView = rootView.findViewById(R.id.textView)


        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val name = user.displayName
            textName.text = "Benvenuto $name"
        }

        Log.e(String.toString(), textName.toString())
        return rootView
    }


}

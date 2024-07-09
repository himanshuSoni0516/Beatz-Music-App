package com.example.beatz

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.beatz.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.regex.Pattern

class SignUpActivity : AppCompatActivity() {

    lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createAccountBtn.setOnClickListener {
            val email = binding.emailEdittext.text.toString()
            val password = binding.passwordEdittext.text.toString()
            val confirmPassword = binding.confirmPasswordEdittext.text.toString()

            if(!Pattern.matches(Patterns.EMAIL_ADDRESS.pattern(),email)) {
                binding.emailEdittext.setError("Invalid email address")
                return@setOnClickListener

            }

            if(password.length<6){
                binding.passwordEdittext.setError("Password must be at least 6 characters")
                return@setOnClickListener
            }

            if(!password.equals(confirmPassword)){
                binding.confirmPasswordEdittext.setError("Password does not match")
                return@setOnClickListener
            }

            createAccountWithFirebase(email,password)
        }
        binding.gotoLoginBtn.setOnClickListener {
            finish()
        }

    }

    fun createAccountWithFirebase(email:String, password:String) {
        setInProgress(true)
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)

            .addOnSuccessListener {
                setInProgress(false)
                Toast.makeText(applicationContext,"Account created successfully",Toast.LENGTH_SHORT).show()
                finish()
            }

            .addOnFailureListener {
                setInProgress(false)
                Toast.makeText(applicationContext,"Failed to create account",Toast.LENGTH_SHORT).show()
            }


    }

    fun setInProgress(inProgress:Boolean){
        if(inProgress){
            binding.progressBar.visibility = View.VISIBLE
            binding.createAccountBtn.visibility = View.GONE
        }else{
            binding.progressBar.visibility = View.GONE
            binding.createAccountBtn.visibility = View.VISIBLE
        }
    }
}
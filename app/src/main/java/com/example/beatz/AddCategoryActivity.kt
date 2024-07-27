package com.example.beatz

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.beatz.databinding.ActivityAddCategoryBinding
import com.example.beatz.databinding.ActivityLoginBinding
import com.example.beatz.databinding.ActivityMainBinding
import com.example.beatz.models.CategoryModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.Manifest
import com.google.firebase.Firebase
import com.google.firebase.storage.storage

class AddCategoryActivity : AppCompatActivity() {
    lateinit var  binding: ActivityAddCategoryBinding
    private lateinit var db: FirebaseFirestore
    private var imageUri: Uri? = null
    private var categoryName: String = ""
    private var categoryUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        binding.btnAddCategory.setOnClickListener {
            categoryName = binding.txtCategoryName.text.toString()
            //val categoryUrl = "https://firebasestorage.googleapis.com/v0/b/beatz-a869e.appspot.com/o/category_images%2Fcat_classic.png.jpg?alt=media&token=5d7c2a63-842b-4dc6-98c7-de28bade2167"
            if (categoryName.isNotEmpty() && categoryUrl.isNotEmpty()) {
                uploadCategory(categoryName, categoryUrl)
            }
        }

        binding.imgCategoryImage.setOnClickListener{
            if(binding.txtCategoryName.text.isEmpty()){
                Toast.makeText(this, "Please enter category name before selecting category image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                if (checkAndRequestPermissions()) {
                    selectImage()
                }
            }

        }



    }

    private fun uploadCategory(name: String, url: String) {
        val category = CategoryModel(name, url, listOf())
        db.collection("category")
            .add(category)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    private fun selectImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            binding.imgCategoryImage.setImageURI(imageUri)
            uploadImageToFirebase()
        }
    }

    private fun uploadImageToFirebase() {
        if (imageUri != null) {
            categoryName = binding.txtCategoryName.text.toString()
            val storageReference = Firebase.storage.reference.child("category_images/cat_${categoryName}")
            storageReference.putFile(imageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    storageReference.downloadUrl.addOnSuccessListener { uri ->
                        //saveImageUriToFirestore(uri.toString())
                        categoryUrl = uri.toString()
                        binding.txtCategoryUrl.setText(categoryUrl)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveImageUriToFirestore(imageUri: String) {

        val imageInfo = hashMapOf(
            "imageUrl" to imageUri
        )

        db.collection("images")
            .add(imageInfo)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Image Uploaded Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to Upload Image URL", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_ID_MULTIPLE_PERMISSIONS)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_ID_MULTIPLE_PERMISSIONS -> {
                val perms: MutableMap<String, Int> = HashMap()
                perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms[Manifest.permission.READ_MEDIA_IMAGES] = PackageManager.PERMISSION_GRANTED
                }

                for (i in permissions.indices) {
                    perms[permissions[i]] = grantResults[i]
                }

                if (perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && perms[Manifest.permission.READ_MEDIA_IMAGES] == PackageManager.PERMISSION_GRANTED)
                ) {
                    selectImage()
                } else {
                    Toast.makeText(
                        this,
                        "Some permissions are not granted. Please enable permissions in settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
        private const val PICK_IMAGE_REQUEST = 1
    }
}
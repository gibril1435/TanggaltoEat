package com.example.appl.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.appl.R
import com.example.appl.data.Recipe
import com.example.appl.utils.FileUtils
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class EditRecipeActivity : AppCompatActivity() {
    private var selectedImageUri: Uri? = null
    private lateinit var imageView: ImageView
    private lateinit var recipeNameInput: TextInputEditText
    private lateinit var authorNameInput: TextInputEditText
    private lateinit var ingredientsInput: TextInputEditText
    private lateinit var toolsInput: TextInputEditText
    private lateinit var stepsInput: TextInputEditText
    private var originalRecipe: Recipe? = null

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(imageView)
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            openImagePicker()
        } else {
            Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_recipe)

        initializeViews()
        loadRecipeData()
    }

    private fun initializeViews() {
        imageView = findViewById<ImageView>(R.id.ivFood).apply {
            setOnClickListener { checkAndRequestPermissions() }
        }
        recipeNameInput = findViewById(R.id.etRecipeName)
        authorNameInput = findViewById(R.id.etAuthorName)
        ingredientsInput = findViewById(R.id.etIngredients)
        toolsInput = findViewById(R.id.etTools)
        stepsInput = findViewById(R.id.etSteps)

        findViewById<Button>(R.id.btnSave).setText("Save Changes")
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveRecipe()
        }
    }

    private fun loadRecipeData() {
        val recipeName = intent.getStringExtra("recipeName")
        val recipeDate = intent.getStringExtra("recipeDate")
        val recipe = FileUtils.loadRecipes().find {
            it.name == recipeName && it.creationDate == recipeDate
        }
        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        originalRecipe = recipe
        recipeNameInput.setText(recipe.name)
        authorNameInput.setText(recipe.author)
        ingredientsInput.setText(recipe.ingredients.joinToString("\n"))
        toolsInput.setText(recipe.tools.joinToString("\n"))
        stepsInput.setText(recipe.steps.joinToString("\n"))
        Glide.with(this)
            .load(File(recipe.imagePath))
            .centerCrop()
            .into(imageView)
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs storage permission to save recipe images")
                .setPositiveButton("Settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(permissions)
            }
            return
        }
        openImagePicker()
    }

    private fun openImagePicker() {
        imagePicker.launch("image/*")
    }

    private fun saveRecipe() {
        val name = recipeNameInput.text?.toString()
        val author = authorNameInput.text?.toString()
        val ingredients = ingredientsInput.text?.toString()
        val tools = toolsInput.text?.toString()
        val steps = stepsInput.text?.toString()
        val original = originalRecipe
        if (name.isNullOrBlank() || author.isNullOrBlank() || ingredients.isNullOrBlank() || 
            tools.isNullOrBlank() || steps.isNullOrBlank() || original == null) {
            Toast.makeText(this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
            return
        }
        // If a new image is selected, copy it, else use the old image path
        val imagePath = if (selectedImageUri != null) {
            FileUtils.copyImageToApp(this, selectedImageUri!!)
        } else {
            original.imagePath
        }
        if (imagePath == null) {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            return
        }
        // Delete the old recipe file and image if a new image is used
        if (selectedImageUri != null) {
            File(original.imagePath).delete()
        }
        File(FileUtils.getDataDirectory(), "${original.creationDate}.txt").delete()
        val updatedRecipe = Recipe(
            name = name,
            author = author,
            imagePath = imagePath,
            ingredients = ingredients.split("\n").filter { it.isNotBlank() },
            tools = tools.split("\n").filter { it.isNotBlank() },
            steps = steps.split("\n").filter { it.isNotBlank() },
            creationDate = original.creationDate
        )
        FileUtils.saveRecipe(updatedRecipe)
        Toast.makeText(this, "Recipe updated successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
} 
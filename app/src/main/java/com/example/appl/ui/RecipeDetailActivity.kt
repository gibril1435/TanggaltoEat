package com.example.appl.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.appl.R
import com.example.appl.utils.FileUtils
import java.io.File

class RecipeDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        // Get recipe details from intent
        val recipeName = intent.getStringExtra("recipeName")
        val recipeDate = intent.getStringExtra("recipeDate")

        if (recipeName == null || recipeDate == null) {
            finish()
            return
        }

        // Load the recipe
        val recipe = FileUtils.loadRecipes().find { 
            it.name == recipeName && it.creationDate == recipeDate 
        }

        if (recipe == null) {
            finish()
            return
        }

        // Set up views
        findViewById<TextView>(R.id.tvRecipeName).text = recipe.name
        findViewById<TextView>(R.id.tvAuthor).text = "by ${recipe.author}"

        // Load recipe image
        Glide.with(this)
            .load(File(recipe.imagePath))
            .centerCrop()
            .into(findViewById(R.id.ivFood))

        // Set up ingredients
        val ingredientsText = StringBuilder().apply {
            append("Ingredients:\n")
            recipe.ingredients.forEach { append("• $it\n") }
        }
        findViewById<TextView>(R.id.tvIngredients).text = ingredientsText

        // Set up tools
        val toolsText = StringBuilder().apply {
            append("Tools:\n")
            recipe.tools.forEach { append("• $it\n") }
        }
        findViewById<TextView>(R.id.tvTools).text = toolsText

        // Set up steps
        val stepsText = StringBuilder().apply {
            append("Steps:\n\n")
            recipe.steps.forEachIndexed { index, step ->
                append("${index + 1}. $step\n\n")
            }
        }
        findViewById<TextView>(R.id.tvSteps).text = stepsText
    }
} 
package com.example.appl.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.appl.data.Recipe
import com.example.appl.utils.FileUtils
import java.io.File

class ComposeHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecipeGrid(recipes = FileUtils.loadRecipes())
                }
            }
        }
    }
}

@Composable
fun RecipeGrid(recipes: List<Recipe>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(recipes) { recipe ->
            RecipeCard(recipe = recipe)
        }
    }
}

@Composable
fun RecipeCard(recipe: Recipe) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                context.startActivity(
                    Intent(context, RecipeDetailActivity::class.java).apply {
                        putExtra("recipeName", recipe.name)
                        putExtra("recipeDate", recipe.creationDate)
                    }
                )
            }
    ) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(File(recipe.imagePath)),
                contentDescription = recipe.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "by ${recipe.author}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
} 
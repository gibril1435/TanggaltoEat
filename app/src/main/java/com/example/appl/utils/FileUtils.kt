package com.example.appl.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.appl.data.Recipe
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    private var dataDirectory: String? = null
    private const val PREFS_NAME = "TanggaltoEatPrefs"
    private const val KEY_DATA_DIRECTORY = "data_directory"
    
    fun initializeDirectory(context: Context) {
        if (dataDirectory == null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DATA_DIRECTORY, null)?.let { setDataDirectory(it) }
        }
    }
    
    fun setDataDirectory(path: String, context: Context? = null) {
        dataDirectory = path
        File(path).mkdirs()
        File("$path/images").mkdirs()
        
        context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.putString(KEY_DATA_DIRECTORY, path)?.apply()
    }

    fun getDataDirectory(): String? = dataDirectory

    fun saveRecipe(recipe: Recipe) {
        dataDirectory?.let { dir ->
            File(dir, "${recipe.creationDate}.txt").writeText("""
                RECIPE_NAME: ${recipe.name}
                AUTHOR: ${recipe.author}
                IMAGE_PATH: ${recipe.imagePath}

                ---INGREDIENTS---
                ${recipe.ingredients.joinToString("\n")}
                ---INGREDIENTS---

                ---TOOLS---
                ${recipe.tools.joinToString("\n")}
                ---TOOLS---

                ---STEPS---
                ${recipe.steps.joinToString("\n")}
                ---STEPS---
            """.trimIndent())
        }
    }

    fun loadRecipes(): List<Recipe> {
        return dataDirectory?.let { dir ->
            File(dir).listFiles { file -> file.extension == "txt" }
                ?.mapNotNull { file ->
                    try {
                        val content = file.readText()
                        val sections = mapOf(
                            "name" to "RECIPE_NAME:\\s*(.+)".toRegex().find(content)?.groupValues?.get(1),
                            "author" to "AUTHOR:\\s*(.+)".toRegex().find(content)?.groupValues?.get(1),
                            "image" to "IMAGE_PATH:\\s*(.+)".toRegex().find(content)?.groupValues?.get(1),
                            "ingredients" to "---INGREDIENTS---\\n([\\s\\S]*?)---INGREDIENTS---".toRegex().find(content)?.groupValues?.get(1),
                            "tools" to "---TOOLS---\\n([\\s\\S]*?)---TOOLS---".toRegex().find(content)?.groupValues?.get(1),
                            "steps" to "---STEPS---\\n([\\s\\S]*?)---STEPS---".toRegex().find(content)?.groupValues?.get(1)
                        )
                        
                        if (sections.values.all { it != null }) {
                            Recipe(
                                name = sections["name"]!!.trim(),
                                author = sections["author"]!!.trim(),
                                imagePath = sections["image"]!!.trim(),
                                ingredients = sections["ingredients"]!!.trim().split("\n").filter { it.isNotBlank() },
                                tools = sections["tools"]!!.trim().split("\n").filter { it.isNotBlank() },
                                steps = sections["steps"]!!.trim().split("\n").filter { it.isNotBlank() },
                                creationDate = file.nameWithoutExtension
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
        } ?: emptyList()
    }

    fun generateFileName(): String = 
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    fun copyImageToApp(context: Context, uri: Uri): String? {
        return dataDirectory?.let { dir ->
            try {
                val fileName = "${generateFileName()}.jpg"
                val imagesDir = File("$dir/images").apply { mkdirs() }
                val destinationFile = File(imagesDir, fileName)
                
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val originalBitmap = BitmapFactory.decodeStream(input)
                    val size = minOf(originalBitmap.width, originalBitmap.height)
                    val x = (originalBitmap.width - size) / 2
                    val y = (originalBitmap.height - size) / 2
                    
                    Bitmap.createBitmap(originalBitmap, x, y, size, size).also { cropped ->
                        FileOutputStream(destinationFile).use { out ->
                            cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        cropped.recycle()
                        originalBitmap.recycle()
                    }
                    
                    "$dir/images/$fileName"
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        dataDirectory?.let { dir ->
            File(dir, "${recipe.creationDate}.txt").delete()
            File(recipe.imagePath).delete()
        }
    }
} 
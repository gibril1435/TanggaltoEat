package com.example.appl.data

data class Recipe(
    val name: String,
    val author: String,
    val imagePath: String,
    val ingredients: List<String>,
    val tools: List<String>,
    val steps: List<String>,
    val creationDate: String // Will be used as file name
) 
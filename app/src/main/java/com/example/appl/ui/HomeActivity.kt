package com.example.appl.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appl.R
import com.example.appl.adapters.RecipeAdapter
import com.example.appl.utils.FileUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class HomeActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private lateinit var emptyStateView: View
    private lateinit var fabDelete: FloatingActionButton
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabFolder: FloatingActionButton
    private lateinit var fabEdit: FloatingActionButton
    private lateinit var fabTiagram: FloatingActionButton
    private var isTiagramMenuOpen = false

    private var deleteMode = false
    private var editMode = false
    private val selectedPositions = mutableSetOf<Int>()
    private var recipes: List<com.example.appl.data.Recipe> = emptyList()

    private val directoryPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { handleDirectorySelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        try {
            setupViews()
            loadRecipes()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupViews() {
        try {
            recyclerView = findViewById<RecyclerView>(R.id.rvRecipes)?.apply {
                layoutManager = GridLayoutManager(this@HomeActivity, 2)
            } ?: throw IllegalStateException("Required view rvRecipes not found")
            
            emptyStateView = findViewById(R.id.emptyState) 
                ?: throw IllegalStateException("Required view emptyState not found")

            fabAdd = findViewById(R.id.fabAdd)
            fabFolder = findViewById(R.id.fabFolder)
            fabDelete = findViewById(R.id.fabDelete)
            fabEdit = findViewById(R.id.fabEdit)
            fabTiagram = findViewById(R.id.fabTiagram)

            // Hide all feature FABs initially
            fabAdd.visibility = View.GONE
            fabFolder.visibility = View.GONE
            fabDelete.visibility = View.GONE
            fabEdit.visibility = View.GONE

            fabTiagram.setOnClickListener {
                toggleTiagramMenu()
            }

            fabAdd.setOnClickListener {
                if (isTiagramMenuOpen) {
                    toggleTiagramMenu()
                }
                if (!deleteMode && !editMode) {
                    startActivity(Intent(this, AddRecipeActivity::class.java))
                }
            }
            fabFolder.setOnClickListener {
                if (isTiagramMenuOpen) {
                    toggleTiagramMenu()
                }
                if (!deleteMode && !editMode) {
                    checkAndRequestPermissions()
                }
            }
            fabDelete.setOnClickListener {
                if (isTiagramMenuOpen) {
                    toggleTiagramMenu()
                }
                if (!deleteMode && !editMode) {
                    enterDeleteMode()
                } else if (deleteMode) {
                    confirmDeleteSelected()
                }
            }
            fabEdit.setOnClickListener {
                if (isTiagramMenuOpen) {
                    toggleTiagramMenu()
                }
                if (!editMode && !deleteMode) {
                    enterEditMode()
                }
            }
        } catch (e: Exception) {
            throw IllegalStateException("Error setting up views: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // Exit any active modes when returning to the activity
            if (editMode) {
                exitEditMode()
            }
            if (deleteMode) {
                exitDeleteMode()
            }
            // Reset the Tiagram menu state
            isTiagramMenuOpen = false
            // Reset all FABs to their initial state
            resetFeatureFabs()
            // Show the Tiagram FAB
            fabTiagram.visibility = View.VISIBLE
            // Load the recipes
            loadRecipes()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading recipes: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkAndRequestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("TanggaltoEat needs storage permission to store recipe data")
                .setPositiveButton("Settings") { _, _ ->
                    try {
                        startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        })
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error opening settings: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        directoryPicker.launch(null)
    }

    private fun handleDirectorySelection(uri: Uri) {
        try {
            val directory = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                DocumentFile.fromTreeUri(this, uri)?.uri?.path?.let { uriPath ->
                    val segments = uriPath.split(":")
                    if (segments.size > 1) {
                        "${Environment.getExternalStorageDirectory()}/${segments[1]}"
                    } else {
                        "${Environment.getExternalStorageDirectory()}/$uriPath"
                    }
                }?.let { File(it) }
            } else {
                uri.path?.let { File(it) }
            }

            directory?.let {
                val tanggaltoEatDir = File(it, "TanggaltoEat/Data")
                FileUtils.setDataDirectory(tanggaltoEatDir.absolutePath, this)
                loadRecipes()
                Toast.makeText(this, "Directory changed successfully", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Failed to access directory", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error selecting directory: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun enterDeleteMode() {
        deleteMode = true
        selectedPositions.clear()
        updateAdapter()
        Toast.makeText(this, "Select recipes to delete", Toast.LENGTH_SHORT).show()
        
        // First hide all FABs except Tiagram
        val fabs = listOf(fabAdd, fabFolder, fabEdit)
        for (fab in fabs) {
            fab.animate()
                .alpha(0f)
                .translationY(0f)
                .setDuration(200)
                .withEndAction { fab.visibility = View.GONE }
                .start()
        }
        
        // Then show delete FAB with animation
        fabDelete.visibility = View.VISIBLE
        fabDelete.alpha = 0f
        fabDelete.translationY = 100f
        fabDelete.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
        
        fabDelete.setImageResource(android.R.drawable.ic_menu_delete)
        isTiagramMenuOpen = false
    }

    private fun exitDeleteMode() {
        deleteMode = false
        selectedPositions.clear()
        updateAdapter()
        
        // First hide delete FAB with animation
        fabDelete.animate()
            .alpha(0f)
            .translationY(100f)
            .setDuration(200)
            .withEndAction {
                fabDelete.visibility = View.GONE
                fabDelete.setImageResource(R.drawable.ic_delete)
                // After delete FAB is hidden, reset everything
                resetFeatureFabs()
                fabTiagram.visibility = View.VISIBLE
            }
            .start()
    }

    private fun enterEditMode() {
        editMode = true
        selectedPositions.clear()
        updateAdapter()
        Toast.makeText(this, "Tap a recipe to edit", Toast.LENGTH_SHORT).show()
        resetFeatureFabs()
        fabTiagram.visibility = View.VISIBLE
        fabEdit.setImageResource(android.R.drawable.ic_menu_edit)
    }

    private fun exitEditMode() {
        editMode = false
        selectedPositions.clear()
        updateAdapter()
        resetFeatureFabs()
        fabTiagram.visibility = View.VISIBLE
        fabEdit.setImageResource(R.drawable.ic_edit)
    }

    private fun resetFeatureFabs() {
        val fabs = listOf(fabAdd, fabFolder, fabDelete, fabEdit)
        for (fab in fabs) {
            fab.animate().cancel()
            fab.visibility = View.GONE
            fab.alpha = 0f
            fab.translationY = 0f
        }
        isTiagramMenuOpen = false
    }

    private fun toggleTiagramMenu() {
        if (isTiagramMenuOpen) {
            // Hide all feature FABs with animation
            fabFolder.animate().translationY(0f).alpha(0f).setDuration(200).withEndAction { fabFolder.visibility = View.GONE }.start()
            fabAdd.animate().translationY(0f).alpha(0f).setDuration(200).withEndAction { fabAdd.visibility = View.GONE }.start()
            fabDelete.animate().translationY(0f).alpha(0f).setDuration(200).withEndAction { fabDelete.visibility = View.GONE }.start()
            fabEdit.animate().translationY(0f).alpha(0f).setDuration(200).withEndAction { fabEdit.visibility = View.GONE }.start()
            isTiagramMenuOpen = false
        } else {
            // Show all feature FABs with vertical spread animation (order: folder, add, delete, edit from bottom)
            fabFolder.visibility = View.VISIBLE
            fabAdd.visibility = View.VISIBLE
            fabDelete.visibility = View.VISIBLE
            fabEdit.visibility = View.VISIBLE
            fabFolder.alpha = 0f; fabAdd.alpha = 0f; fabDelete.alpha = 0f; fabEdit.alpha = 0f
            val spacing = fabTiagram.height + 32f
            fabFolder.animate().translationY(-spacing * 1).alpha(1f).setDuration(200).start()
            fabAdd.animate().translationY(-spacing * 2).alpha(1f).setDuration(250).start()
            fabDelete.animate().translationY(-spacing * 3).alpha(1f).setDuration(300).start()
            fabEdit.animate().translationY(-spacing * 4).alpha(1f).setDuration(350).start()
            isTiagramMenuOpen = true
        }
    }

    override fun onBackPressed() {
        if (isTiagramMenuOpen) {
            toggleTiagramMenu()
            return
        }
        if (deleteMode) {
            exitDeleteMode()
        } else if (editMode) {
            exitEditMode()
        } else {
            super.onBackPressed()
        }
    }

    private fun confirmDeleteSelected() {
        if (selectedPositions.isEmpty()) {
            Toast.makeText(this, "No recipes selected", Toast.LENGTH_SHORT).show()
            exitDeleteMode()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Recipes")
            .setMessage("Are you sure you want to delete the selected recipe(s)?")
            .setPositiveButton("Delete") { _, _ ->
                val toDelete = selectedPositions.map { recipes[it] }
                toDelete.forEach { com.example.appl.utils.FileUtils.deleteRecipe(it) }
                exitDeleteMode()
                loadRecipes()
                Toast.makeText(this, "Deleted ${toDelete.size} recipe(s)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ -> exitDeleteMode() }
            .show()
    }

    private fun updateAdapter() {
        adapter = RecipeAdapter(
            recipes = recipes,
            onItemClick = { recipe ->
                if (editMode) {
                    val intent = Intent(this, EditRecipeActivity::class.java).apply {
                        putExtra("recipeName", recipe.name)
                        putExtra("recipeDate", recipe.creationDate)
                    }
                    startActivity(intent)
                } else if (!deleteMode) {
                    startActivity(Intent(this, RecipeDetailActivity::class.java).apply {
                        putExtra("recipeName", recipe.name)
                        putExtra("recipeDate", recipe.creationDate)
                    })
                }
            },
            onItemLongClick = { pos ->
                if (!deleteMode && !editMode) {
                    enterDeleteMode()
                    selectedPositions.add(pos)
                    adapter.notifyItemChanged(pos)
                }
            },
            isDeleteMode = { deleteMode },
            isSelected = { pos -> selectedPositions.contains(pos) },
            onSelectToggle = { pos ->
                if (selectedPositions.contains(pos)) selectedPositions.remove(pos)
                else selectedPositions.add(pos)
                adapter.notifyItemChanged(pos)
            }
        )
        recyclerView.adapter = adapter
    }

    private fun loadRecipes() {
        try {
            recipes = com.example.appl.utils.FileUtils.loadRecipes()
            updateAdapter()
            emptyStateView.visibility = if (recipes.isEmpty()) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading recipes: ${e.message}", Toast.LENGTH_LONG).show()
            emptyStateView.visibility = View.VISIBLE
        }
    }
} 
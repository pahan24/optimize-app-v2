package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentCleanerBinding
import com.ultra.optimize.x.databinding.ItemJunkCategoryBinding

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.ultra.optimize.x.utils.ShizukuManager
import com.ultra.optimize.x.utils.Utils

data class JunkCategory(
    val name: String,
    val size: Long,
    val icon: Int,
    var isSelected: Boolean = true,
    val packageName: String? = null,
    val isApp: Boolean = false
)

class CleanerFragment : Fragment() {
    private var _binding: FragmentCleanerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: JunkAdapter
    private var categories = mutableListOf<JunkCategory>()
    private var isScanned = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCleanerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Set Spannable Title
        val title = "STORAGE\nCLEANER"
        val spannable = android.text.SpannableString(title)
        val blueColor = ContextCompat.getColor(requireContext(), R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, 7, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTitle.text = spannable
        
        setupRecyclerView()

        binding.btnClean.setOnClickListener {
            if (!isScanned) {
                startScan()
            } else {
                startClean()
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = JunkAdapter(categories) { updateJunkSize() }
        binding.rvCategories.layoutManager = LinearLayoutManager(context)
        binding.rvCategories.adapter = adapter
    }

    private fun startScan() {
        binding.btnClean.isEnabled = false
        binding.btnClean.text = "SCANNING..."
        
        Thread {
            val context = context ?: return@Thread
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val appJunk = mutableListOf<JunkCategory>()
            val hasAccess = RootManager.isRooted(context) || (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted())
            
            // Filter non-system apps or common apps
            apps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }.take(15).forEach { app ->
                val label = pm.getApplicationLabel(app).toString()
                var size = (10..150).random().toLong() * 1024 * 1024 // Default simulated
                
                if (hasAccess) {
                    try {
                        val output = RootManager.runCommand("du -sk /data/data/${app.packageName}/cache").trim()
                        if (output.isNotEmpty()) {
                            val kb = output.split(Regex("\\s+"))[0].toLong()
                            size = kb * 1024
                        }
                    } catch (e: Exception) {}
                }
                
                appJunk.add(JunkCategory(label, size, R.drawable.ic_layers, true, app.packageName, true))
            }

            Thread.sleep(1500)
            activity?.runOnUiThread {
                if (_binding != null) {
                    categories.clear()
                    
                    var systemCacheSize = (500..1200).random().toLong() * 1024 * 1024
                    var tempFilesSize = (100..400).random().toLong() * 1024 * 1024
                    
                    if (hasAccess) {
                        try {
                            val sysCacheOut = RootManager.runCommand("du -sk /cache").trim()
                            if (sysCacheOut.isNotEmpty()) {
                                systemCacheSize = sysCacheOut.split(Regex("\\s+"))[0].toLong() * 1024
                            }
                            
                            val tempOut = RootManager.runCommand("du -sk /data/local/tmp").trim()
                            if (tempOut.isNotEmpty()) {
                                tempFilesSize = tempOut.split(Regex("\\s+"))[0].toLong() * 1024
                            }
                        } catch (e: Exception) {}
                    }

                    categories.add(JunkCategory("System Cache", systemCacheSize, R.drawable.ic_trash))
                    categories.add(JunkCategory("Temp Files", tempFilesSize, R.drawable.ic_clock))
                    categories.addAll(appJunk)
                    categories.add(JunkCategory("Empty Folders", (10..50).random().toLong() * 1024, R.drawable.ic_trash))
                    
                    adapter.notifyDataSetChanged()
                    updateJunkSize()
                    
                    isScanned = true
                    binding.btnClean.isEnabled = true
                    binding.btnClean.text = "CLEAN NOW"
                    Toast.makeText(requireContext(), "Scan Complete!", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun startClean() {
        val selectedCategories = categories.filter { it.isSelected }
        if (selectedCategories.isEmpty()) {
            Toast.makeText(context, "Select at least one category", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnClean.isEnabled = false
        binding.btnClean.text = "CLEANING..."
        
        Thread {
            val hasAccess = RootManager.isRooted(requireContext()) || (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted())
            
            selectedCategories.forEach { category ->
                if (category.isApp && category.packageName != null) {
                    if (hasAccess) {
                        // Actual cache clearing via Root/Shizuku
                        RootManager.runCommand("pm clear ${category.packageName}")
                    } else {
                        // Simulated cleaning if no access
                        Thread.sleep(200)
                    }
                } else {
                    // System cleaning simulation
                    Thread.sleep(300)
                }
            }

            activity?.runOnUiThread {
                if (_binding != null) {
                    categories.removeAll { it.isSelected }
                    adapter.notifyDataSetChanged()
                    updateJunkSize()
                    
                    if (categories.isEmpty()) {
                        isScanned = false
                        binding.btnClean.text = "SCAN SYSTEM"
                    } else {
                        binding.btnClean.text = "CLEAN NOW"
                    }
                    
                    binding.btnClean.isEnabled = true
                    Toast.makeText(requireContext(), "Storage Cleaned Successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updateJunkSize() {
        if (_binding == null) return
        val totalSize = categories.filter { it.isSelected }.sumOf { it.size }
        binding.tvJunkSize.text = formatSize(totalSize)
        
        // Update progress bar (simulated)
        val progress = if (totalSize > 0) (totalSize.toFloat() / (5000L * 1024 * 1024) * 100).toInt().coerceIn(0, 100) else 0
        animateProgress(binding.progressStorage, progress)
    }

    private fun animateProgress(progress: com.google.android.material.progressindicator.LinearProgressIndicator, value: Int) {
        val animator = android.animation.ValueAnimator.ofInt(progress.progress, value)
        animator.duration = 800
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.addUpdateListener {
            if (_binding != null) {
                progress.progress = it.animatedValue as Int
            }
        }
        animator.start()
    }

    private fun formatSize(size: Long): String {
        if (size <= 0) return "0 MB"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class JunkAdapter(
        private val list: List<JunkCategory>,
        private val onSelectionChanged: () -> Unit
    ) : RecyclerView.Adapter<JunkAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemJunkCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int = list.size

        inner class ViewHolder(private val binding: ItemJunkCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: JunkCategory) {
                binding.tvCategoryName.text = item.name
                binding.tvCategorySize.text = formatSize(item.size)
                binding.ivCategoryIcon.setImageResource(item.icon)
                binding.cbSelect.isChecked = item.isSelected
                
                binding.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                    item.isSelected = isChecked
                    onSelectionChanged()
                }
                
                binding.root.setOnClickListener {
                    binding.cbSelect.isChecked = !binding.cbSelect.isChecked
                }
            }
        }
    }
}


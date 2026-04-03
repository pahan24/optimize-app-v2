package com.ultra.optimize.x.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentFileManagerBinding
import com.ultra.optimize.x.databinding.ItemCloudFileBinding
import com.ultra.optimize.x.utils.Constants
import com.ultra.optimize.x.utils.SettingsManager
import java.util.*

data class CloudFile(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val date: Long = 0L
)

class FileManagerFragment : Fragment() {

    private var _binding: FragmentFileManagerBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance(Constants.FIRESTORE_DATABASE_ID)
    private lateinit var adapter: FileAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        if (SettingsManager.isAdmin(requireContext())) {
            binding.adminPanel.visibility = View.VISIBLE
        }

        setupRecyclerView()
        loadFiles()

        binding.btnAddFile.setOnClickListener {
            val name = binding.etFileName.text.toString().trim()
            val url = binding.etFileUrl.text.toString().trim()

            if (name.isEmpty() || url.isEmpty()) {
                Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addFileToCloud(name, url)
        }
    }

    private fun setupRecyclerView() {
        adapter = FileAdapter { file -> downloadFile(file) }
        binding.rvFiles.layoutManager = LinearLayoutManager(context)
        binding.rvFiles.adapter = adapter
    }

    private fun loadFiles() {
        binding.progressFiles.visibility = View.VISIBLE
        db.collection("cloud_files")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                binding.progressFiles.visibility = View.GONE
                if (e != null) {
                    Toast.makeText(context, "Error loading files: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val files = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CloudFile::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                adapter.submitList(files)
            }
    }

    private fun addFileToCloud(name: String, url: String) {
        val file = hashMapOf(
            "name" to name,
            "url" to url,
            "date" to System.currentTimeMillis()
        )

        db.collection("cloud_files").add(file)
            .addOnSuccessListener {
                Toast.makeText(context, "File added successfully", Toast.LENGTH_SHORT).show()
                binding.etFileName.text?.clear()
                binding.etFileUrl.text?.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to add file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun downloadFile(file: CloudFile) {
        try {
            val request = DownloadManager.Request(Uri.parse(file.url))
                .setTitle(file.name)
                .setDescription("Downloading file from Ultra Optimize X")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.name)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class FileAdapter(private val onDownload: (CloudFile) -> Unit) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private var files = listOf<CloudFile>()

    fun submitList(newList: List<CloudFile>) {
        files = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemCloudFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    inner class FileViewHolder(private val binding: ItemCloudFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: CloudFile) {
            binding.tvFileName.text = file.name
            binding.tvFileDate.text = "Added: ${java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(file.date))}"
            binding.btnDownload.setOnClickListener { onDownload(file) }
        }
    }
}

package com.rotationcontrol.force.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.rotationcontrol.force.R
import com.rotationcontrol.force.data.RotationMode
import com.rotationcontrol.force.databinding.ItemAppProfileBinding

internal data class AppProfileItem(
    val packageName: String,
    val label: String,
    val selectedModeId: Int?
)

internal class AppProfileAdapter(
    context: Context,
    modes: List<RotationMode>,
    onProfileSelected: (packageName: String, modeId: Int?) -> Unit
) : ListAdapter<AppProfileItem, AppProfileAdapter.ProfileViewHolder>(DIFF) {
    private var onProfileSelected: ((packageName: String, modeId: Int?) -> Unit)? = onProfileSelected
    private val inflater = LayoutInflater.from(context)
    private val packageManager = context.packageManager
    private val options = listOf(
        ProfileOption(null, "Global", "Use the rotation selected on Control")
    ) + modes.map { ProfileOption(it.id, it.title, it.description) }
    private val iconCache = object : LruCache<String, Drawable>(32) {}
    private val iconExecutor: ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "app-icon-loader").apply { isDaemon = true }
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemAppProfileBinding.inflate(inflater, parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewDetachedFromWindow(holder: ProfileViewHolder) {
        holder.dismissDropdown()
        super.onViewDetachedFromWindow(holder)
    }

    override fun onViewRecycled(holder: ProfileViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        clearResources()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun clearResources() {
        iconExecutor.shutdownNow()
        iconCache.evictAll()
        onProfileSelected = null
    }

    internal inner class ProfileViewHolder(
        private val binding: ItemAppProfileBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var boundItem: AppProfileItem? = null
        private val optionAdapter = ProfileOptionAdapter(binding.profileDropdown.context, options)

        init {
            binding.profileDropdown.apply {
                threshold = 0
                setDropDownBackgroundResource(R.drawable.bg_dropdown_popup)
                dropDownVerticalOffset = dp(4)
                setDropDownHeight(dp(320))
                setAdapter(optionAdapter)
                setOnItemClickListener { _, _, index, _ ->
                    val item = boundItem ?: return@setOnItemClickListener
                    val option = optionAdapter.getItem(index) ?: return@setOnItemClickListener
                    optionAdapter.selectedModeId = option.modeId
                    optionAdapter.notifyDataSetChanged()
                    binding.profileDropdown.setText(option.label, false)
                    boundItem = item.copy(selectedModeId = option.modeId)
                    onProfileSelected?.invoke(item.packageName, option.modeId)
                }
            }
        }

        fun bind(item: AppProfileItem) {
            boundItem = item
            binding.appName.text = item.label
            binding.packageName.text = item.packageName
            binding.appIcon.tag = item.packageName
            val cachedIcon = iconCache.get(item.packageName)
            if (cachedIcon != null) binding.appIcon.setImageDrawable(cachedIcon)
            else {
                binding.appIcon.setImageDrawable(null)
                loadIconAsync(item.packageName, binding.appIcon)
            }
            optionAdapter.selectedModeId = item.selectedModeId
            optionAdapter.notifyDataSetChanged()
            val selectedLabel = options.firstOrNull { it.modeId == item.selectedModeId }?.label ?: "Global"
            binding.profileDropdown.setText(selectedLabel, false)
        }

        fun dismissDropdown() {
            binding.profileDropdown.dismissDropDown()
        }

        fun recycle() {
            dismissDropdown()
            boundItem = null
        }

        private fun loadIconAsync(packageName: String, imageView: android.widget.ImageView) {
            val viewRef = WeakReference(imageView)
            val pm = packageManager
            val cache = iconCache
            val handler = mainHandler
            runCatching {
                iconExecutor.execute {
                    val drawable = cache.get(packageName) ?: runCatching {
                        pm.getApplicationIcon(packageName).also { cache.put(packageName, it) }
                    }.getOrNull()
                    if (drawable != null) {
                        handler.post {
                            val target = viewRef.get()
                            if (target?.tag == packageName) target.setImageDrawable(drawable)
                        }
                    }
                }
            }
        }

        private fun dp(value: Int): Int =
            (value * binding.root.resources.displayMetrics.density).toInt()
    }

    private data class ProfileOption(val modeId: Int?, val label: String, val description: String) {
        override fun toString(): String = label
    }

    private inner class ProfileOptionAdapter(
        context: Context,
        private val options: List<ProfileOption>
    ) : ArrayAdapter<ProfileOption>(context, 0, options) {
        var selectedModeId: Int? = null

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View = bindOptionRow(position, convertView, parent)

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View = bindOptionRow(position, convertView, parent)

        private fun bindOptionRow(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView ?: inflater.inflate(R.layout.item_rotation_option, parent, false)
            val option = getItem(position) ?: return row
            row.findViewById<TextView>(R.id.optionTitle).text = option.label
            row.findViewById<TextView>(R.id.optionDescription).text = option.description
            row.findViewById<TextView>(R.id.optionSelected).visibility =
                if (option.modeId == selectedModeId) View.VISIBLE else View.GONE
            return row
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppProfileItem>() {
            override fun areItemsTheSame(oldItem: AppProfileItem, newItem: AppProfileItem) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: AppProfileItem, newItem: AppProfileItem) =
                oldItem == newItem
        }
    }
}

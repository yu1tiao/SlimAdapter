package com.yu1tiao.slimadapter

import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.util.isNotEmpty
import com.yu1tiao.slimadapter.core.*
import com.yu1tiao.slimadapter.diff.DefaultDiffCallback
import com.yu1tiao.slimadapter.diff.SlimDiffUtil


/**
 * @author yuli
 * @date 2021/5/20
 * @description SlimAdapter
 */
open class SlimAdapter<T> : AbsAdapter<T>() {

    /**
     * key: viewType (实际上直接使用的layoutId作为viewType)    value: [ViewInjector]
     */
    private val viewInjectors by lazy { SparseArray<ViewInjector<T>>() }
    private var injectorFinder: InjectorFinder<T>? = null
    private var onItemClickListener: OnItemClickListener? = null
    private var onItemLongClickListener: OnItemLongClickListener? = null

    override fun getItemViewType(position: Int): Int {
        require(viewInjectors.isNotEmpty()) {
            "No view type registered."
        }

        // 注册ViewInjector时，key保存的就是layoutId
        // 如果只有1个viewType，直接返回layoutId
        // 否则通过injectorFinder获取返回的layoutId，也通过layoutId获取到对应的ViewInjector

        if (viewInjectors.size() > 1) {
            // 注册了多个ViewInjector
            require(injectorFinder != null) {
                "Multiple view types are registered. You must set a injectorFinder"
            }
            return injectorFinder!!.layoutId(getItem(position), position, itemCount)
        }

        return viewInjectors.keyAt(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 这里viewType就是layoutId
        val itemView = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        val holder = ViewHolder(itemView)
        setupItemClickListener(holder)
        setupItemLongClickListener(holder)
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val viewType = getItemViewType(position)
        val injector = viewInjectors[viewType]
        requireNotNull(injector) {
            "viewType出错，注册多个ViewInjector时，必须注册injectorFinder并且返回正确的layoutId"
        }
        injector.bind(holder, getItem(position), position)
    }

    private fun setupItemClickListener(viewHolder: ViewHolder) {
        viewHolder.itemView.setOnClickListener {
            val position = viewHolder.layoutPosition
            onItemClickListener?.invoke(position, mDataSet[position] as Any)
        }
    }

    private fun setupItemLongClickListener(viewHolder: ViewHolder) {
        viewHolder.itemView.setOnLongClickListener {
            val position = viewHolder.layoutPosition
            onItemLongClickListener?.invoke(position, mDataSet[position] as Any)
            true
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    fun register(injector: ViewInjector<T>) {
        // 直接使用layoutId当做viewType
        viewInjectors.put(injector.layoutId, injector)
    }

    fun register(
        @LayoutRes layoutId: Int,
        block: (holder: ViewHolder, item: T, position: Int) -> Unit
    ) {
        register(object : ViewInjector<T>(layoutId) {
            override fun bind(holder: ViewHolder, item: T, position: Int) {
                block.invoke(holder, item, position)
            }
        })
    }

    fun injectorFinder(finder: InjectorFinder<T>) {
        this.injectorFinder = finder
    }

    fun injectorFinder(finder: (item: T, position: Int, itemCount: Int) -> Int) {
        this.injectorFinder = object : InjectorFinder<T> {
            override fun layoutId(item: T, position: Int, itemCount: Int): Int {
                return finder(item, position, itemCount)
            }
        }
    }

    fun itemClickListener(listener: OnItemClickListener) {
        this.onItemClickListener = listener
    }

    fun itemLongClickListener(listener: OnItemLongClickListener) {
        this.onItemLongClickListener = listener
    }

    fun autoDiff(diffCallback: SlimDiffUtil.Callback? = DefaultDiffCallback()) {
        this.diffCallback = diffCallback
    }
}


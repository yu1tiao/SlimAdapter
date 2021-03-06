package com.mathew.slimadapter.ex.loadmore

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mathew.slimadapter.AttachToRecyclerViewListener
import com.mathew.slimadapter.util.SlimUtil
import java.lang.ref.WeakReference

/**
 * @author yu
 * @date 2018/1/12
 */

interface LoadMoreListener {
    fun onLoadMore()
}

open class MoreLoader(
    private val mLoadMoreListener: LoadMoreListener?,
    private val mLoadMoreFooter: ILoadMoreFooter
) : RecyclerView.OnScrollListener(), AttachToRecyclerViewListener {

    @Volatile
    var enable = true

    @Volatile
    var isContentEmpty = true

    private var itemCountCallback: (() -> Int)? = null
    private var recyclerView: WeakReference<RecyclerView>? = null

    fun setItemCountCallback(callback: () -> Int) {
        this.itemCountCallback = callback
    }

    val loadMoreFooterView: View
        get() = mLoadMoreFooter.view

    fun loadMoreCompleted() {
        mLoadMoreFooter.status = ILoadMoreFooter.Status.COMPLETED
    }

    fun loadMoreError() {
        mLoadMoreFooter.status = ILoadMoreFooter.Status.ERROR
    }

    fun noMore() {
        mLoadMoreFooter.status = ILoadMoreFooter.Status.NO_MORE
    }

    fun disableIfNotFullPage() {

        val recyclerView = this.recyclerView?.get() ?: return
        val manager = recyclerView.layoutManager ?: return

        recyclerView.postDelayed({
            val itemCount = itemCountCallback?.invoke() ?: return@postDelayed
            val index = SlimUtil.findLastCompletelyVisibleItemPosition(manager)
            enable = index + 1 > itemCount
        }, 100)
    }

    init {
        mLoadMoreFooter.view.setOnClickListener {
            if (mLoadMoreFooter.status == ILoadMoreFooter.Status.ERROR) {
                mLoadMoreListener?.onLoadMore()
                mLoadMoreFooter.status = ILoadMoreFooter.Status.LOADING
            }
        }
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        if (!enable || isContentEmpty) {
            return
        }
        when (newState) {
            RecyclerView.SCROLL_STATE_IDLE -> {
                if (mLoadMoreListener == null ||
                    mLoadMoreFooter.status != ILoadMoreFooter.Status.COMPLETED
                ) {
                    return
                }

                val layoutManager = recyclerView.layoutManager ?: return

                val lastPosition = SlimUtil.findLastCompletelyVisibleItemPosition(layoutManager)

                if (layoutManager.childCount > 0 && lastPosition >= layoutManager.itemCount - 1) {
                    mLoadMoreFooter.status = ILoadMoreFooter.Status.LOADING
                    mLoadMoreListener.onLoadMore()
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = WeakReference(recyclerView)
        recyclerView.addOnScrollListener(this)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnScrollListener(this)
    }
}

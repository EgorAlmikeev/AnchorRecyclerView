package your.package.here

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AnchorRecyclerView<ItemViewHolder : RecyclerView.ViewHolder, Item>(
    context: Context,
    attributeSet: AttributeSet
) : RecyclerView(context, attributeSet) {

    /**
     * returns a ListArray<MyModel?> of your models, which you need to display
     */
    var dataProvider: () -> ArrayList<Item?> = { ArrayList() }

    /**
     * downloads or generates a new pack of models and pushes it into a list of models returned by myDataProvider
     */
    var dataLoader: (
        context: Context,
        anchor: Item?, pageSize: Int, preExecuteCallback: (() -> Unit)?,
        postExecuteCallback: (result: Any) -> Unit
    ) -> Unit = { _, _, _, preExecuteCallback, postExecuteCallback ->
        preExecuteCallback?.invoke()
        postExecuteCallback.invoke(true)
    }

    private var anchor: Item? = null
    private var pageSize = 10
    private var isLoadingData = false
    private var dataSet: ArrayList<Item?> = ArrayList()

    init {
        layoutManager = LinearLayoutManager(context)

        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (!isLoadingData) {
                    if (linearLayoutManager.findLastCompletelyVisibleItemPosition() == adapter?.itemCount?.minus(
                            1
                        )
                    ) {
                        loadNextPage()
                    }
                }
            }
        })
    }

    fun setPageSize(pageSize: Int) {
        this.pageSize = pageSize
        restartPaging()
        loadNextPage()
    }

    fun restartPaging() {
        anchor = null
        dataSet.clear()
        refreshAdapterDataSet()
        adapter?.notifyDataSetChanged()
    }

    fun loadNextPage() {
        dataLoader(
            context,
            when {
                dataSet.isEmpty() -> null
                else -> dataSet.last()
            }, pageSize,
            {
                isLoadingData = true
                displayLoadingElement()
            },
            {
                dataSet.clear()
                dataSet.addAll(dataProvider.invoke())
                refreshAdapterDataSet()
                adapter?.notifyDataSetChanged()
                isLoadingData = false
            }
        )
    }

    private fun displayLoadingElement() {
        dataSet.add(null)
        refreshAdapterDataSet()
        adapter?.notifyItemInserted(dataSet.size - 1)
        scrollToPosition(dataSet.size - 1)
    }

    private fun refreshAdapterDataSet() {
        if (adapter != null)
            (adapter as PagingAdapter<ItemViewHolder, Item>).replaceDataSet(dataSet)
    }

    /**
     * @param itemViewLayoutId – id of your item layout
     * @param itemViewHolderCreator – returns an instance of your ItemViewHolder by view
     * @param itemViewHolderBinder – binds your ItemViewHolder and your ItemModel
     */
    class PagingAdapter<ItemViewHolder : ViewHolder, ItemModel>(
        private val itemViewLayoutId: Int,
        private val itemViewHolderCreator: (view: View) -> ItemViewHolder,
        private val itemViewHolderBinder: (holder: ItemViewHolder, itemModel: ItemModel) -> Unit
    ) : Adapter<ViewHolder>() {

        private var dataSet: ArrayList<ItemModel?> = ArrayList()

        fun replaceDataSet(newData: ArrayList<ItemModel?>) {
            dataSet.clear()
            dataSet.addAll(newData)
        }

        override fun getItemViewType(position: Int): Int {
            if (dataSet[position] == null)
                return LOAD_VIEW_TYPE
            return ITEM_VIEW_TYPE
        }

        class LoadViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            private var progressBar: ProgressBar = ProgressBar(view.context)

            init {
                if (view is LinearLayout) {
                    view.addView(progressBar)
                    view.orientation = LinearLayout.VERTICAL
                    view.gravity = Gravity.CENTER

                    view.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return if (viewType == ITEM_VIEW_TYPE)
                itemViewHolderCreator.invoke(
                    LayoutInflater.from(parent.context).inflate(
                        itemViewLayoutId,
                        parent,
                        false
                    )
                )
            else
                LoadViewHolder(LinearLayout(parent.context))
        }

        override fun getItemCount() = dataSet.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            if (holder.itemViewType == ITEM_VIEW_TYPE) {
                itemViewHolderBinder.invoke(
                    holder as ItemViewHolder,
                    dataSet[position]!!
                )
            }
        }

        companion object {
            const val ITEM_VIEW_TYPE = 0
            const val LOAD_VIEW_TYPE = 1
        }
    }
}

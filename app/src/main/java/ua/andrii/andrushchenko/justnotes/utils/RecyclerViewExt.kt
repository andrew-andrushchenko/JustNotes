package ua.andrii.andrushchenko.justnotes.utils

import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

fun RecyclerView.setupLinearLayoutManager(
    @Px margin: Int,
    recyclerViewOrientation: Int = RecyclerView.VERTICAL
) {
    layoutManager = LinearLayoutManager(context, recyclerViewOrientation, false)
    addItemDecoration(
        LinearLayoutSpacingDecoration(
            margin,
            recyclerViewOrientation
        )
    )
}

fun RecyclerView.setupStaggeredGridLayoutManager(
    @Px margin: Int,
    recyclerViewOrientation: Int = RecyclerView.VERTICAL
) {
    val decorationIndex = 0
    val spanCount = if (resources.configuration.orientation == ORIENTATION_LANDSCAPE) 3 else 2
    layoutManager = StaggeredGridLayoutManager(spanCount, recyclerViewOrientation)

    (layoutManager as? StaggeredGridLayoutManager)?.apply {
        this.spanCount = spanCount
        gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
    }

    if (itemDecorationCount != 0) {
        removeItemDecorationAt(decorationIndex)
    }

    addItemDecoration(
        StaggeredGridItemOffsetDecoration(
            margin,
            spanCount
        ),
        decorationIndex
    )
}

private class LinearLayoutSpacingDecoration(
    @Px private val margin: Int,
    private val recyclerViewOrientation: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val position = parent.getChildAdapterPosition(view)

        when (recyclerViewOrientation) {
            RecyclerView.VERTICAL -> {
                outRect.apply {
                    top = if (position == 0) margin else 0
                    bottom = margin
                    right = margin
                    left = margin
                }
            }
            RecyclerView.HORIZONTAL -> {
                outRect.apply {
                    top = margin
                    bottom = margin
                    right = margin
                    left = if (position == 0) margin else 0
                }
            }
        }
    }
}

private class StaggeredGridItemOffsetDecoration(
    private val margin: Int,
    private val spanCount: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val params = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        val spanIndex = params?.spanIndex ?: 0
        val position = params?.viewAdapterPosition ?: 0

        outRect.apply {
            top = if (position < spanCount) margin else 0
            left = if (spanIndex == 0) margin else 0
            right = margin
            bottom = margin
        }
    }
}
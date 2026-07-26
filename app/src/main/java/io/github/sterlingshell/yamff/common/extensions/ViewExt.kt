package io.github.sterlingshell.yamff.common.extensions

import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.resetAdapter() {
    this.adapter = adapter
}

fun Resources.Theme.getAttr(@AttrRes id: Int) =
    TypedValue().apply { resolveAttribute(id, this, true) }

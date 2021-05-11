package com.reach5.identity.sdk.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.reach5.identity.sdk.core.Provider

class ProvidersAdapter(private val context: Context, private var providers: List<Provider>) :
    BaseAdapter() {
    fun refresh(providers: List<Provider>) {
        this.providers = providers
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): Provider {
        return providers[position]
    }

    override fun getItemId(position: Int): Long {
        return providers[position].name.hashCode().toLong()
    }

    override fun getCount(): Int {
        return providers.size
    }

    private class ViewHolder(row: View?) {
        var name: TextView? = null

        init {
            this.name = row?.findViewById(R.id.name)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View?
        val viewHolder: ViewHolder
        if (convertView != null) {
            view = convertView
            viewHolder = view.tag as ViewHolder
        } else {
            val inflater =
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            view = inflater.inflate(R.layout.provider_item, null)
            viewHolder = ViewHolder(view)
            view?.tag = viewHolder
        }

        val provider = providers[position]

        viewHolder.name?.text = provider.name

        return view as View
    }
}

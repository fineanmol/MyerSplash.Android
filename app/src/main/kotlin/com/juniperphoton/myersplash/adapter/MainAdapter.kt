package com.juniperphoton.myersplash.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.juniperphoton.myersplash.App
import com.juniperphoton.myersplash.R
import com.juniperphoton.myersplash.fragment.ImageListFragment
import com.juniperphoton.myersplash.model.UnsplashCategory

@Suppress("DEPRECATION")
class MainAdapter(
    fm: FragmentManager
) : FragmentStatePagerAdapter(fm) {
    companion object {
        private val titles = listOf(
            UnsplashCategory.NEW_CATEGORY_ID to R.string.pivot_new,
            UnsplashCategory.RANDOM_CATEGORY_ID to R.string.pivot_random,
            UnsplashCategory.DEVELOPER_ID to R.string.pivot_developer,
            UnsplashCategory.HIGHLIGHTS_CATEGORY_ID to R.string.pivot_highlights
        )
    }

    override fun getItem(position: Int): Fragment {
        return ImageListFragment.build(
            titles[position].first
        )
    }

    override fun getCount(): Int = titles.size

    override fun getPageTitle(position: Int): CharSequence {
        return App.instance.getString(titles[position].second)
    }
}
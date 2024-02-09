package com.example.android.uamp.viewmodels

import dagger.assisted.AssistedFactory

@AssistedFactory
interface MediaItemViewModelFactory {
    fun build(mediaId: String): MediaItemFragmentViewModel
}
TODOs
=====

This file captures the high level goals of the project. This provides guidance for anyone who wants
to contribute. If you see something in the list that you'd like to work on,
the best approach would be to [create an
issue](https://github.com/googlesamples/android-UniversalMusicPlayer/issues) first,
and then provide a pull request once completed to have your work merged into the project.

Please only supply Kotlin PRs to the 'next' branch.

Service Side Tasks
------------------

- Implement rating (ideally "favorite" vs "thumbs up/down").
- Provide integration with the Google Assistant.

UI Tasks
--------

- Implement logic for handling [FLAG_BROWSABLE](https://developer.android.com/reference/android/support/v4/media/MediaBrowserCompat.MediaItem.html#FLAG_BROWSABLE).
- Implement a "now playing" UI with current position and skip forward/back 30s ([BottomSheet](https://material.io/guidelines/components/bottom-sheets.html#bottom-sheets-persistent-bottom-sheets)).

Large UI Tasks
--------------

- Add UI module for Android Wear.
- Add UI module for Android TV with Leanback.

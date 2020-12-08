# Frequently Asked Questions

## How can I change the music which UAMP plays?
UAMP reads its [music catalog](https://storage.googleapis.com/uamp/catalog.json) from a server. 
This contains a list of songs and their metadata in JSON format. To change the music you can create your own 
music catalog file, host it on a server and update the catalog URL in 
[`MusicService`](https://github.com/android/uamp/blob/6c3ff3779d02f55661c5b9d6032cfac507a8415e/common/src/main/java/com/example/android/uamp/media/MusicService.kt#L127). 

Alternatively, you could package your own music catalog and songs inside the app so they can be played without needing an internet connection.
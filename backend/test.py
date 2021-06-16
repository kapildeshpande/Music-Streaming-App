import os

import audio_metadata

def get_metadata(file, file_url):
    metadata = audio_metadata.load(file_url)
    metadata = metadata.__dict__
    obj = {}
    obj['filesize'] = metadata['filesize']
    # obj['pictures'] = metadata['pictures']
    obj['duration'] = metadata['streaminfo']['duration']
    metadata['tags'] = metadata['tags'].__dict__
    if metadata['tags'].get('title'):
        obj['title'] =  metadata['tags']['title']
    else:
        obj['title'] = file

    if metadata['tags'].get('album'):
        obj['album'] =  metadata['tags']['album']
    else:
        obj['album'] = file

    if metadata['tags'].get('artist'):
        obj['artist'] =  metadata['tags']['artist']
    else:
        obj['artist'] = file
    obj['url'] = file_url
    return obj


video_title = "Maroon 5 - Girls Like You ft.mp3"
url = "music/Maroon 5 - Girls Like You ft.mp3"
print(get_metadata("hello.mp3", "music/hello.mp3"))
print(get_metadata(video_title, url))
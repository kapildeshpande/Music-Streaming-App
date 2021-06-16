import audio_metadata
import os

def get_metadata(file, file_url):
    obj = {}
    try:
        metadata = audio_metadata.load(file_url)
        metadata = metadata.__dict__
        obj['filesize'] = metadata['filesize']
        # obj['pictures'] = metadata['pictures']
        obj['duration'] = metadata['streaminfo']['duration']*1000
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
    except:
        obj['filesize'] = os.path.getsize(file_url)
        import librosa
        obj['duration'] = int(librosa.get_duration(filename='Dhaaga.mp3')*1000)
        obj['title'] = file
        obj['album'] = file
        obj['artist'] = file
        obj['url'] = file_url
        return obj

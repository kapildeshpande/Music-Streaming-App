from __future__ import unicode_literals
import os
import json
from flask import Blueprint
from model import db, User, History
from flask import abort, request
from werkzeug.utils import secure_filename
from os import path
from config import *
import youtube_dl
from model import db, Music
from helper import get_metadata
import threading
import asyncio

admin = Blueprint('admin', __name__, url_prefix='/admin')

@admin.route('/db_data')
def admin_login():
    username = request.form.get('username')
    password = request.form.get('password')
    if username is None or password is None or username != "admin" and password != "1234":
        abort(400)    # missing arguments    
    users = User.query.all()
    songs = Music.query.all()
    history = History.query.all()
    json_string = json.dumps([user.as_dict() for user in users],indent=4, sort_keys=True, default=str) 
    json_string += json.dumps([music.as_dict() for music in songs],indent=4, sort_keys=True, default=str)
    json_string += json.dumps([h.as_dict() for h in history],indent=4, sort_keys=True, default=str)
    # print(json_string) 
    return json_string, 200

def add_music_to_db(file_path, url):
    obj = get_metadata(file_path, url)

    music = Music(title=obj['title'], album=obj['album'], artist=obj['artist'],
            url=url, duration=obj['duration'], filesize=obj['filesize'])
    db.session.add(music)
    db.session.commit()

@admin.route('/upload', methods = ['POST'])
def admin_file_upload():
    try:
        username = request.form.get('username')
        password = request.form.get('password')
        if username is None or password is None or username != "admin" and password != "1234":
            return 'Authentication Failed',400    # missing arguments
        f = request.files['file']
        if f is None:
            return 'File not found',400
        file_path = secure_filename(f.filename)
        file_ext = os.path.splitext(file_path)[1]
        if file_ext not in extensions_allowed:
            return "Invalid song", 400
        
        if path.exists(upload_folder) is False:
            os.mkdir(upload_folder)

        url = os.path.join(upload_folder, file_path)
        f.save(url)
        threading.Thread(add_music_to_db(file_path, url)).start()

        return 'file uploaded', 200
    except:
        abort(400)

def download_from_youtube(url):
    try:
        with youtube_dl.YoutubeDL() as ydl:
            info_dict = ydl.extract_info(url, download=False)
            video_title = info_dict.get('title', None)
        video_title = video_title.partition('.')[0] + '.mp3'
        ydl_opts = {
            'format': 'bestaudio/best',
            'addmetadata':True,
            'outtmpl': video_title,
            'postprocessors': [{
                'key': 'FFmpegExtractAudio',
                'preferredcodec': 'mp3',
                'preferredquality': '192',
            }]
        }
        with youtube_dl.YoutubeDL(ydl_opts) as ydl:
            info_dict = ydl.extract_info(url, download=True)

        new_video_title = video_title.partition('.')[0]
        os.rename(video_title, 'music/{0}.mp3'.format(new_video_title));
        
        return new_video_title + '.mp3'
    except:
        return False

#use music.youtube URL and remove playlist portion of url
@admin.route('/upload_from_youtube', methods = ['POST'])
def admin_youtube_upload():
    try:
        username = request.form.get('username')
        password = request.form.get('password')
        if username is None or password is None or username != "admin" and password != "1234":
            return 'Authentication Failed',400    # missing arguments
        url = request.form.get('url')
        if url is None:
            return 'Invalid url', 400
        video_title = download_from_youtube(url)
        if video_title is False:
            abort(400)
        url = 'music/{0}'.format(video_title)
        threading.Thread(add_music_to_db(video_title, url)).start()
        return 'file uploaded', 200
    except:
        abort(400)
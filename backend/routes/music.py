from flask import Blueprint, request
from flask.wrappers import JSONMixin
from werkzeug.utils import secure_filename
from flask import abort, Response, jsonify, request, g, url_for
import os
from os import path
from config import *
from os import listdir
import json
import mimetypes
import re
from model import db, Music, User, History
from helper import get_metadata
from flask_httpauth import HTTPBasicAuth

music = Blueprint('music', __name__, url_prefix='/music')

auth = HTTPBasicAuth()

@auth.verify_password
def verify_password(username_or_token, password):
    # first try to authenticate by token
    user = User.verify_auth_token(username_or_token)
    if not user:
        # try to authenticate with username/password
        user = User.query.filter_by(username=username_or_token).first()
        if not user or not user.verify_password(password):
            return False
    g.user = user
    return True

@music.after_request
def after_request(response):
    response.headers.add('Accept-Ranges', 'bytes')
    return response

def send_file_partial(path):
    range_header = request.headers.get('Range', None)
    if not range_header: return False
    
    size = os.path.getsize(path)    
    byte1, byte2 = 0, None
    
    m = re.search('(\d+)-(\d*)', range_header)
    g = m.groups()
    
    if g[0]: byte1 = int(g[0])
    if g[1]: byte2 = int(g[1])

    length = size - byte1
    if byte2 is not None:
        length = byte2 - byte1
    
    data = None
    def generate():
        length = size - byte1
        if byte2 is not None:
            length = byte2 - byte1
        with open(path, 'rb') as f:
            f.seek(byte1)
            # data = f.read(length)
            data = f.read(1024)
            while data and length > 0:
                yield data
                data = f.read(1024)
                length -= 1024

    return Response(generate(), 
        206,
        mimetype=mimetypes.guess_type(path)[0], 
        headers=[
                    ('Content-Range', 'bytes {0}-{1}/{2}'.format(byte1, byte1 + length - 1, size))
                ],
        direct_passthrough=True)

def get_song(music_id):
    try:
        music_id = secure_filename(music_id)
        music = Music.query.filter_by(title=music_id).first()
        file_path = music.url
        if path.exists(file_path) is False:
            abort(400)
        
        def generate():
            with open(file_path, "rb") as fwav:
                data = fwav.read(1024)
                while data:
                    yield data
                    data = fwav.read(1024)

        response = send_file_partial(file_path)
        if response is False:
            return Response(generate(), mimetype="audio/mpeg")
        return response
    except:
        abort(400)

@music.route('/<music_id>')
@auth.login_required
def get_song_with_login(music_id):
    #add history
    music = Music.query.filter_by(title=music_id).first()
    history = History(music.id, g.user.id)
    db.session.add(history)
    db.session.commit()
    return get_song(music_id)

@music.route('/guest/<music_id>')
def get_song_without_login(music_id):
    return get_song(music_id)


@music.route('/display_all')
def get_all_songs():
    try:
        songs = Music.query.all()
        json_string = json.dumps([music.as_dict() for music in songs],indent=4, sort_keys=True, default=str) 
        return json_string, 200
    except:
        abort(400)
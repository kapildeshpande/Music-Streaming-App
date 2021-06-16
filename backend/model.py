from config import *
import datetime
import time
import jwt
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash

db = SQLAlchemy()

class Music(db.Model):
    __tablename__ = 'music'
    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(128), index=True)
    album = db.Column(db.String(128))
    artist = db.Column(db.String(128))
    url = db.Column(db.String(128))
    duration = db.Column(db.Integer)
    filesize = db.Column(db.Integer)
    created_at = db.Column(db.DateTime, default=datetime.datetime.utcnow)

    def __init__(self,title,album,artist,url,duration,filesize):
        self.title = title
        self.album = album
        self.artist = artist
        self.url = url
        self.duration = duration
        self.filesize = filesize

    def __repr__(self):
        return '<id {}>'.format(self.id)

    def as_dict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}

class User(db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(32), index=True)
    password_hash = db.Column(db.String(128))
    created_at = db.Column(db.DateTime, default=datetime.datetime.utcnow)

    def hash_password(self, password):
        self.password_hash = generate_password_hash(password)

    def verify_password(self, password):
        return check_password_hash(self.password_hash, password)

    def generate_auth_token(self, expires_in=600):
        return jwt.encode(
            {'id': self.id, 'exp': time.time() + expires_in},
            secret_key, algorithm='HS256')
    
    def __repr__(self):
           return f"<id={self.id}, username={self.username}, password={self.password_hash}, created_at={self.created_at}>"
    
    def as_dict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}

    @staticmethod
    def verify_auth_token(token):
        try:
            data = jwt.decode(token, secret_key,
                              algorithms=['HS256'])
        except:
            return
        return User.query.get(data['id'])



class History(db.Model):
    __tablename__ = 'history'
    music_id = db.Column(db.Integer, db.ForeignKey('music.id'), primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), primary_key=True)
    created_at = db.Column(db.DateTime, default=datetime.datetime.utcnow, primary_key=True)

    def __init__(self, music_id, user_id):
        self.music_id = music_id
        self.user_id = user_id

    def __repr__(self):
        return '<music_id {0} user_id {1}>'.format(self.music_id, self.user_id)

    def as_dict(self):
        return {c.name: getattr(self, c.name) for c in self.__table__.columns}

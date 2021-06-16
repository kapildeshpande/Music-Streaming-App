import os
from flask import Flask, abort, request, jsonify, g, url_for
from os import path
from config import *
# from flask_migrate import Migrate
from model import db
from routes.music import music
from routes.user import user
from routes.admin import admin

# initialization
app = Flask(__name__)
app.config['SECRET_KEY'] = secret_key
app.config['SQLALCHEMY_DATABASE_URI'] = 'postgres://ekidmiviojpcwn:a03c7021ab53537480f751ba1ee40c451be8c05854f1989f7375ab22779dd9b2@ec2-52-86-2-228.compute-1.amazonaws.com:5432/d7tiaar023ev6t'
app.config['SQLALCHEMY_COMMIT_ON_TEARDOWN'] = True
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

app.config['MAX_CONTENT_LENGTH'] = file_mb_max * 1024 * 1024 # max 10 mb song
app.config['UPLOAD_EXTENSIONS'] = extensions_allowed
app.config['UPLOAD_PATH'] = upload_folder

app.register_blueprint(music)
app.register_blueprint(user)
app.register_blueprint(admin)

db.init_app(app)
with app.app_context():
    db.create_all()

if __name__ == '__main__':
    app.run(debug=True)
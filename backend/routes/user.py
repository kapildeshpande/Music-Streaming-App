from flask import Blueprint
from model import db, User, History
from flask import Flask, abort, request, jsonify, g, url_for, Response
from flask_httpauth import HTTPBasicAuth
import json

user = Blueprint('user', __name__, url_prefix='/user')

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


def get_token(user):
    return user.generate_auth_token(600)

@user.route('/signup', methods=['POST'])
def new_user():
    username = request.json.get('username')
    password = request.json.get('password')
    if username is None or password is None or len(password) < 6:
        abort(400)    # missing arguments
    if User.query.filter_by(username=username).first() is not None:
        abort(400)    # existing user
    user = User(username=username)
    user.hash_password(password)
    db.session.add(user)
    db.session.commit()
    token = get_token(user)
    return jsonify({'token': token.decode('ascii'), 'duration': 600})

@user.route('/login')
def login():
    username = request.headers.get('username', None)
    password = request.headers.get('password', None)
    if username is None or password is None:
        abort(400)    # missing arguments
    user = User.query.filter_by(username=username).first()
    if not user or not user.verify_password(password):
        abort(400)
    token = get_token(user)
    return jsonify({'token': token.decode('ascii'), 'duration': 600})

@user.route('/login_with_token')
@auth.login_required
def login_with_token():
    return jsonify({'username': g.user.username}), 200

@user.route('/history')
@auth.login_required
def get_user_history():
    history = History.query.filter_by(user_id=g.user.id).all()
    json_str =  json.dumps([h.as_dict() for h in history],indent=4, sort_keys=True, default=str)
    return json_str, 200
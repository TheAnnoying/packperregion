import sqlite3
from uuid import uuid4
import time
from flask import Flask, request, jsonify
import os

database_file = "data.db"

if not os.path.isdir("packs"):
    os.mkdir("packs")

with sqlite3.connect(database_file) as connection:
    cursor = connection.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS packs(
            token TEXT primary key,
            pack_name TEXT,
            creation_timestamp INTEGER,
            uuid TEXT
        )
    """)

app = Flask(__name__, static_folder="packs")


@app.after_request
def cors_headers(response):
    response.headers["Access-Control-Allow-Origin"] = "*"
    response.headers["Access-Control-Allow-Headers"] = "Content-Type"
    response.headers["Access-Control-Allow-Methods"] = "GET"
    return response


@app.route("/gettoken", methods=["GET"])
def generate_token():
    token = str(uuid4())
    uuid = request.args.get("uuid")

    if uuid:
        with sqlite3.connect(database_file) as connection:
            cursor = connection.cursor()
            cursor.execute("""
                INSERT INTO packs (token, pack_name, creation_timestamp, uuid)
                VALUES (?, ?, ?, ?)
            """, (token, None, int(time.time()), uuid))

            connection.commit()
        return jsonify({"token": token})
    else:
        return "Not authorized"


@app.route("/uploadpack", methods=["POST"])
def copy_pack():
    token = request.args.get("token")
    if not os.path.isfile(f"packs/{token}"):
        uploaded_file = request.files["pack"]
        uploaded_file.save(f"packs/{token}.zip")
        with sqlite3.connect(database_file) as connection:
            cursor = connection.cursor()
            cursor.execute("""
                UPDATE packs
                SET pack_name = ?
                WHERE token = ?
            """, (uploaded_file.filename, request.args.get("token")))

            connection.commit()
        return jsonify({"success": True})
    else:
        return jsonify({"success": False})


@app.route("/getpackdata", methods=["GET"])
def get_pack_data():
    token = request.args.get("token")
    file_exists = os.path.isfile(f"packs/{token}")
    with sqlite3.connect(database_file) as connection:
        cursor = connection.cursor()
        cursor.execute("""
            SELECT *
            FROM packs
            WHERE token = ?
        """, [token])

        data = cursor.fetchone()
        if data:
            return jsonify(dict(zip([description[0] for description in cursor.description], data)))
        else:
            return jsonify({})


@app.route("/deletepack", methods=["DELETE"])
def delete_pack():
    token = request.args.get("token")
    if os.path.isfile(f"packs/{token}"):
        os.remove(f"packs/{token}")
    with sqlite3.connect(database_file) as connection:
        cursor = connection.cursor()
        cursor.execute("""
            SELECT *
            FROM packs
            WHERE token = ?
        """, [token])

        if cursor.fetchone():
            cursor.execute("""
                DELETE FROM packs
                WHERE token = ?
            """, [token])

            connection.commit()
            return jsonify({"success": True})
        else:
            return jsonify({"success": False})


app.run(port=8080)

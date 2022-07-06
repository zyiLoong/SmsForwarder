#!/usr/bin/python3
from flask import Flask, request, Response

default_token="EQ3GYenEeHCg8fBB6eA4"
url='http://10.236.207.117:8000/sms?token=Bk677n4W'

app = Flask(__name__)


@app.route('/sms/get')
def index():
    token = request.args.get('token')
    if default_token!=token:
        print(f"token 不可用: {token}")
        return Response(status=400)

    return url


@app.route('/sms/set')
def set():
    global url
    print(request.args)
    token = request.args.get('token')
    if default_token!=token:
        print(f"token 不可用: {token}")
        return Response(status=400)
    new_url = request.args.get('url')
    print(f"new_url: {new_url}")
    if new_url is None:
        print("入参url为空")
        return Response(status=400)
    url = new_url
    return url

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8081, debug=False)

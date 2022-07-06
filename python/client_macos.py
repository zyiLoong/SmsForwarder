#!/usr/bin/python3
from flask import Flask, request, Response
import re
import os

p = re.compile('(?<=验证码.)\d{6}')
default_token="Bk677n4W"

app = Flask(__name__)


@app.route('/sms')
def index():
    token = request.args.get('token')
    if default_token!=token:
        print(f"token 不可用: {token}")
        return Response(status=400)

    msg = request.args.get('msg')
    print(msg)
    list = re.findall(p, msg)
    if len(list) != 1:
        print(f'验证码解析出错，解析结果: {list}')
        return Response(status=400)
    print(f'验证码解析结果为: {list[0]}')
    # 发送通知消息
    os.system(
        f"""osascript -e 'display notification "{list[0]}" with title "统一管理平台登录验证码"  '""")
    # 复制到粘贴板
    os.system(f'echo {list[0]} | pbcopy')
    return "done"


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8000, debug=False)

> Webhook 类型的发送通道需要填写一个固定的 url, 当需要发送信息时回去调用这个 url  
> 但是存在一个问题: 这个 url 地址是我笔记本上开启的服务，笔记本连接 WIFI 时IP地址是会变的，就会造成调用失败  
> 修改了Webhook发送通道逻辑，可以从一个接口动态获取要调用的url，具体看前面的commit
> 这里的3个脚本就是刷新url地址用的

* server.py 
这个脚本放置在一个IP/域名固定的中间服务器上, 提供修改 url 和获取 url 的两个接口

* refresh_url.py
这个脚本用于获取本机IP，并组装要调用的 url, 然后调用 server.py 接口将最新的 url 推送上去

* client_macos.py
这个脚本启动一个 web 服务，接收来自Smsforwarod 转发过来的短信。将短信中的验证码解析出来，发送通知，并将验证码写到系统剪切板中

#!/usr/bin/python3
import socket
import re
import requests
import os

server_token = 'EQ3GYenEeHCg8fBB6eA4'
server_domain = 'www.example.com'

ip_pattern = re.compile('^(\d{1,3}.){3}\d{1,3}$')

ip = socket.gethostbyname(socket.getfqdn(socket.gethostname()))

if re.search(ip_pattern,ip):
	local_url = f'http://{ip}:8000/sms?token=Bk677n4W'
	encoded_local_url=requests.utils.quote(local_url)
	
	server_url = f'http://{server_domain}/sms/set?token={server_token}&url={encoded_local_url}'
	print(server_url)
	response = requests.get(server_url)
	print(response.text)
else:
	os.system(f"""osascript -e 'display notification "{ip}" with title "获取本机IP失败"  '""")

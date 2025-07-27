import requests

proxy_host = "127.0.0.1"
proxy_port = 8080
proxy_url = f"http://{proxy_host}:{proxy_port}"

proxies = {
    "http": proxy_url,
    "https": proxy_url  # only needed if you're testing https
}

url = "http://httpbin.org/post"
data = {"name": "test"}

try:
    response = requests.post(url, data=data, proxies=proxies, timeout=10)
    print("Status code:", response.status_code)
    print("Response JSON:", response.json())
except requests.exceptions.RequestException as e:
    print("Proxy POST request failed:", e)
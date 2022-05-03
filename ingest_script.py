import requests
import json
from requests_toolbelt.multipart.encoder import MultipartEncoder
import configparser

pid = "123456789/24"

#FAIR check
print("\n######### FAIR Checking ##########\n")
body = {
  "id": pid,
  "repo": "dspace7",
  "oai_base": "",
  "lang": "ES"
}
url = "http://dspace-fair:9090/v1.0/rda/rda_all"
result = requests.post(url, data = json.dumps(body), headers={'Content-Type': 'application/json'}, verify=False)
print(result.content)


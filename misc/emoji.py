import base64
import os

import requests

jojo = requests.get(
    "https://www.zhihu.com/api/v4/sticker-groups/1114161698310770688"
).json()['data']['stickers']

mapping = {item['placeholder']: 'emoji_' + item['id'] + '.png' for item in jojo}
with open('emoji_mapping.json', 'w', encoding='utf-8') as f:
    import json
    json.dump(mapping, f, ensure_ascii=False, indent=4)

os.makedirs('emojis', exist_ok=True)

for item in jojo:
    content = base64.b64decode(item['static_image_url'][len('data:image/png;base64,'):])
    with open(os.path.join('emojis', 'emoji_' + item['id'] + '.png'), 'wb') as f:
        f.write(content)

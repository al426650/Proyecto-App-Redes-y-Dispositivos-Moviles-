import json
import requests

def request_yolo(file_path):
# Run inference on an image
    url = "https://predict.ultralytics.com"
    headers = {"x-api-key": "ec9d387a6216fb0a2403f147717b8936"}
    data = {"model": "https://hub.ultralytics.com/models/Pi3uAeiO8JYD2bX0wotZ", "imgsz": 640, "conf": 0.25, "iou": 0.45}
    #file_path = file_path
    with open(file_path, "rb") as f:
        print(file_path)
        #print(requests.post(url, headers=headers, data=data, files={"file": f}))
        response = requests.post(url, headers=headers, data=data, files={"file": f})
        print(response)

    # Check for successful response
    response.raise_for_status()
    # Print inference results

    return response.json()
    #print(json.dumps(response2.json(), indent=2))

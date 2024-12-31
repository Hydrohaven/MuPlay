from fastapi import FastAPI

app = FastAPI()

latest_intent = {"intent": None}

@app.post("/post")
async def receive_transcription(data: dict):
    """
    Posts transcription data from the client.
    """
    global latest_intent

    intent = data.get("intent")
    if not intent:
        return {"intent": "No transcription provided"}
    
    latest_intent["intent"] = intent
    return {"intent": intent}

@app.get("/get")
async def get_transcription():
    """
    Gets transcription data from the client
    """
    if latest_intent["intent"] == None:
        return {"intent": "Invalid command"}
    return latest_intent

from fastapi import FastAPI

app = FastAPI()

@app.post("/post")
async def receive_transcription(data: dict):
    """
    Receives transcription data from the client.
    """
    intent = data.get("intent")
    if not intent:
        return {"intent": "No transcription provided"}
    return {"intent": intent}

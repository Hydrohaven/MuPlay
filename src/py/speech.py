import pvporcupine
import pyaudio
import struct
import whisper
import wave
import requests  # For Spotify API calls (in Go backend)
import numpy as np

import os
from dotenv import load_dotenv
load_dotenv()

import warnings
warnings.filterwarnings("ignore", category=FutureWarning)
warnings.filterwarnings("ignore", category=UserWarning)


ACCESS_KEY = os.environ["PORCUPINE_ACCESS_KEY"]
SAMPLE_RATE = 16000   # 16 kHz
FILE_NAME = 's.wav'
THRESHOLD = 500       # Amplitude threshold for detecting silence
SILENCE_DURATION = 2  # Duration of silence (in seconds) to stop recording

print('=====================')
print('  Loading Porcupine  ')
print('=====================')
# Initialize Porcupine with your access key and wake word
porcupine = pvporcupine.create(
    access_key=ACCESS_KEY,
    keyword_paths=["src/res/hey-muplay-windows.ppn"] # Path to your wake word model
)

# PyAudio Setup
p = pyaudio.PyAudio()
stream = p.open(
    format=pyaudio.paInt16,                   # 16-bit audio format
    channels=1,                               # Mono audio (1 channel)
    rate=SAMPLE_RATE,                         # Sampling rate required by Porcupine (~16k Hz for porcupine)
    input=True,                               # Capture input
    frames_per_buffer=porcupine.frame_length  # Number of audio samples per frame (~512 for porcupine)
)
print(' Setting Up PyAudio  ')
print('=====================')

# Load Whisper model
whisper_model = whisper.load_model("base.en")
print('   Loading Whisper   ')
print('=====================')

def _record_audio() -> bytes:
    """Record audio until silence. Return audio as bytes."""

    audio_frames = []
    silent_chunks = 0 # count for consecutive silent chunks
    max_silent_chunks = int(SAMPLE_RATE / 1024 * SILENCE_DURATION)  # Chunks required for silence detection

    record_stream = p.open(
        format=pyaudio.paInt16,
        channels=1,
        rate=SAMPLE_RATE,
        input=True,
        frames_per_buffer=1024
    )

    try:
        while True:
            # Read audio chunk from microphone
            data = record_stream.read(1024)
            audio_frames.append(data)

            # Convert audio data to NumPy array for amplitude analysis
            audio_samples = np.frombuffer(data, dtype=np.int16)
            max_amplitude = np.max(np.abs(audio_samples))

            # Check if the chunk is silent
            if max_amplitude < THRESHOLD:
                silent_chunks += 1
            else:
                silent_chunks = 0  # Reset silent_chunks if voice activity resumes

            # Stop recording if silence persists for the required duration
            if silent_chunks > max_silent_chunks:
                print("Silence detected. Stopping recording.")
                break
    finally:
        # Stop Recording
        record_stream.stop_stream()
        record_stream.close()

    
    return b"".join(audio_frames) # joining a list of binary data together

def _save_audio_to_file(audio_data) -> None:
    """Save raw audio data to a WAV file."""

    with wave.open(FILE_NAME, 'wb') as wf:
        wf.setnchannels(1)  # Mono
        wf.setsampwidth(p.get_sample_size(pyaudio.paInt16))
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(audio_data)

def _transcribe_audio(filename) -> str:
    """Transcribe audio using Whisper."""
    
    result = whisper_model.transcribe(filename)
    return result['text']

def listen(func) -> None:
    """
    Main function of the speech component. Listens for wake words and 
    records subsequent audio until silence is detected.

    Doesn't return anything, but constantly sends transcribed data to parameter func.

    Parameters
    ----------
    func : function(str)
        A function that takes in a string of transcribed data.
    """

    try:
        print("Listening for wake word...")
        while True:
            # Read audio data from the microphone, captures one frame (512 samples) from the microphone
            audio_data = stream.read(porcupine.frame_length)
            
            # Convert raw audio bytes to 16-bit integers (required by porcupine)
            audio_samples = struct.unpack_from("h" * porcupine.frame_length, audio_data)
            
            # If the wake word is detected
            if porcupine.process(audio_samples) != -1:
                print("Wake word detected! Speak now!")

                recorded_audio = _record_audio()
                _save_audio_to_file(recorded_audio)
                
                transcription = _transcribe_audio(FILE_NAME)
                print("Transcription: ", transcription)
                
                command = func(transcription)
                # send_to_backend(command)

                if command == 'Shut down':
                    print('Shuting Down...')
                    break
                else:
                    print("Listening for wake word...")
    except KeyboardInterrupt:
        print("Stopping...")
    finally:
        # Cleanup resources
        os.remove(FILE_NAME)
        stream.stop_stream()
        stream.close()
        p.terminate()
        porcupine.delete()


def send_to_backend(intent):
    try:
        response = requests.post("http://localhost:8000/command", json={"intent": intent})
        response.raise_for_status()  # Raise an error for HTTP status codes 4xx/5xx
        print("Response from server:", response.text)
    except requests.exceptions.RequestException as e:
        print("Error sending request:", e)


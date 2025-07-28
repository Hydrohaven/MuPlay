# μPlay
## Description
μPlay is a voice-controlled music player utilizing a wide range of technologies that I have never used before. This project is a remake of my very first project, titled MuPlay-Prototype on my GitHub. 

## Usage & Setup
Setup guide because I keep forgetting how to do it

### Step 1: Create Python Virtual Environment
````bash
python -m venv env
````
### Step 2: Activate Virtual Environment
````bash
env\Scripts\activate
````
### Step 3: Install Python Dependencies
````bash
pip install -r requirements.txt
````
### Step 4: Install Go Dependencies
````bash
go mod tidy
````
### Step 5: Setup FFmpeg (for Whisper)
Install [ffmpeg](https://ffmpeg.org/download.html), either essentails or full, both should work (I used essentials)
````md
# Adding ffmpeg to path
Win+R - Opens System Properties
Click 'Advanced', then 'Environment Variables'
Open Path in System Variables (bottom section)
Click 'New' and add 'C:\ffmpeg\bin'
````
Restart workspace after setting this up
### Step 6: Run Program
In root - Runs main Python file
````bash
py ./src/py/main.py
````
In root - Compiles and runs all Go files
````bash
go run ./src/go
````

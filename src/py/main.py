import speech
import intent
import logging

logging.basicConfig(level=logging.INFO)

if __name__ == "__main__":
    speech.listen(intent.output_command)

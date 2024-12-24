# print('=====================')
print(' Importing Libraries ')
print('=====================')

from sentence_transformers import SentenceTransformer, util

# Load a lightweight Sentence Transformer model
print('   Loading Model...  ')
print('=====================')
model = SentenceTransformer('all-MiniLM-L6-v2')
print('    Model loaded!    ')
print('=====================\n')


SIM_THRESHOLD = 0.65

# Predefined commands and their embeddings, python sends to go hashmap that stores <phrase, function> pairs 
commands = [
    "Play the music",
    "Pause the music",
    "Skip the song",
    "Play the last song",
    "Increase the volume",
    "Increase the volume slightly",
    "Decrease the volume",
    "Decreate the volume slightly",
    "Turn shuffle on",
    "Shut down"
]

# Embed predefined commands
command_embeddings = model.encode(commands)
# print(command_embeddings)


def output_command(phrase: str) -> str:
    """
    Main function of NLP component. Deciphers intent of parameter phrase using 
    Hugging Face Sentence Tansformers, prints closest matching command (65% or higher).

    Parameters
    ----------
    phrase : str
        A string of transcribed text.

    """

    # Simulate user input (e.g., from Whisper transcription)
    user_input = phrase

    # Embed the user input
    user_input_embedding = model.encode(user_input)

    # Find the closest command using cosine similarity
    similarities: util.Tensor = util.cos_sim(user_input_embedding, command_embeddings)
    best_match_idx = similarities.argmax()
    sim_list: list[float] = list(similarities)[0]
    # print(sim_list := list(similarities)[0])

    # Output the identified command, choses one with a 65% match or higher
    command = commands[best_match_idx] if sim_list[best_match_idx] >= SIM_THRESHOLD else "Invalid command"
    print("Closest Command:", command, '\n')
    return command

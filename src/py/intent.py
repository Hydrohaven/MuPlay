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
    "Play the song",
    "Unpause the song",

    "Pause the song",

    "Skip the song",

    "Play the last song",
    "Play the previous song",

    "Increase the volume",
    "Turn up the volume",

    "Decrease the volume",
    "Turn down the volume",

    "Turn shuffle on",
    
    "Shut down"
]

# Next line of commands to be encoded and compared to if the initial comparison fails
next_commands = {
    "Play the song" : ['Can you please play the song',
                        'Play this song',
                        'Would you play the song',
                        'Can you play this song',
                        'Play the song please',
                        'Could you play the song',
                        'Please play the song',
                        'Let\'s play the song',
                        'Start playing the song',
                        'Can you start the song',
                        'Can you unpause the song',
                        'Unpause this song',
                        'Please unpause the song',
                        'Can you please unpause the song',
                        'Unpause the song now',
                        'Could you unpause the song',
                        'Let\'s unpause the song',
                        'Unpause and play the song',
                        'Unpause it and play the song',
                        'Start playing the song again'],

    "Pause the song" : ['Can you pause the song',
                        'Pause this song',
                        'Please pause the song',
                        'Pause the song for me',
                        'Could you pause the song',
                        'Pause the song now',
                        'Pause this track',
                        'Stop and pause the song',
                        'Pause the song for a moment',
                        'Just pause the song'],

    "Skip the song" : ['Can you skip this song',
                        'Skip this song please',
                        'Please skip this song',
                        'Could you skip this song',
                        'Skip this track',
                        'Go ahead and skip this song',
                        'Move on and skip this song',
                        'Skip the current song',
                        'Just skip this song',
                        'Skip to the next song'],

    "Play the last song" : ['Can you play the last song',
                            'Play the last song again',
                            'Please play the last song',
                            'Play the previous song',
                            'Play the last track',
                            'Could you play the last song',
                            'Play the song before this one',
                            'Play the last song for me',
                            'Go back and play the last song',
                            'Play the last tune'], 

    "Increase the volume" : ['Can you increase the volume',
                              'Increase the volume please',
                              'Please increase the volume',
                              'Turn up the volume',
                              'Could you increase the volume',
                              'Raise the volume',
                              'Increase the volume now',
                              'Boost the volume',
                              'Make it louder by increasing the volume',
                              'Just increase the volume'],
                              
    "Decrease the volume" : ['Can you decrease the volume',
                              'Decrease the volume please',
                              'Please decrease the volume',
                              'Turn down the volume',
                              'Could you decrease the volume',
                              'Lower the volume',
                              'Decrease the volume now',
                              'Drop the volume a bit',
                              'Make it quieter by decreasing the volume',
                              'Just decrease the volume'],

    "Turn shuffle on" : ['Can you turn shuffle on',
                           'Turn shuffle on please',
                           'Please turn shuffle on',
                           'Could you turn shuffle on',
                           'Activate shuffle mode',
                           'Turn shuffle on now',
                           'Start shuffle mode',
                           'Switch shuffle on',
                           'Turn on shuffle mode',
                           'Just turn shuffle on'],
    
    "Shut down" : ['Can you shut down',
                    'Shut down the system',
                    'Please shut down',
                    'Could you shut down',
                    'Shut down now',
                    'Shut everything down',
                    'Just shut down',
                    'Power off and shut down',
                    'End everything and shut down',
                    'Turn everything off and shut down']
}

next_commands["Unpause the song"] = next_commands["Play the song"]
next_commands["Play the previous song"] = next_commands["Play the last song"]
next_commands["Turn up the volume"] = next_commands["Increase the volume"]
next_commands["Turn down the volume"] = next_commands["Decrease the volume"]


# Embed predefined commands
command_embeddings = model.encode(commands)


def output_command(phrase: str) -> str:
    """
    Main function of NLP component. Deciphers intent of parameter phrase using 
    Hugging Face Sentence Tansformers, prints closest matching command (65% or higher).

    Args
    phrase (str) : A string of transcribed text.

    """

    # Simulate user input (e.g., from Whisper transcription)
    user_input = phrase

    # Embed the user input
    user_input_embedding = model.encode(user_input)

    # Find the closest command using cosine similarity
    similarities: util.Tensor = util.cos_sim(user_input_embedding, command_embeddings)
    best_match_idx = similarities.argmax()
    sim_list: list[float] = list(similarities)[0]
    print(sim_list := list(similarities)[0])

    # Output the identified command, choses one with a 65% match or higher
    command = commands[best_match_idx] if sim_list[best_match_idx] >= SIM_THRESHOLD else next_closest(commands[best_match_idx], user_input_embedding)

    print("Closest Command:", command, '\n')
    return command

def next_closest(closest: str, user_embedding: util.Tensor) -> str:
    """
    Determines the next closest command if the initial user input fails to pass the threshold.
    Chooses the highest number below the threshold and tries their sub commands with a match level of 60%

    Args
    closest (str) : Closest command phrase below threshold
    user_embedding (Tensor) : Embedding from initial user input
    """

    next_embeddings = model.encode(next_commands[closest])
    similarities: util.Tensor = util.cos_sim(user_embedding, next_embeddings)
    best_match_idx = similarities.argmax()
    sim_list: list[float] = list(similarities)[0]
    print(sim_list := list(similarities)[0])

    # Chooses previous closest command if sub commands can reach 65%
    command = closest if sim_list[best_match_idx] >= SIM_THRESHOLD - 0.05 else 'Invalid Command'

    return command
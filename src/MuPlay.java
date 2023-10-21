package src;
import javafx.embed.swing.JFXPanel;
import edu.cmu.sphinx.api.*;
import javafx.scene.media.*; //Says never used but is needed
import java.io.*;
//import javax.swing.*; //Says never used but is needed

public class MuPlay {
    private LiveSpeechRecognizer speech;
	private Configuration config;

    public static void main(String[] args) throws IOException {
        new MuPlay();
    }

    public MuPlay() throws IOException {
        //Setup
		MuPlayGUI mpg = new MuPlayGUI();
		JFXPanel jfxp = new JFXPanel();
		mpg.getJFrame().add(jfxp);

        Commands mpc = new Commands(mpg.getJFrame(), mpg.getFolder());
		mpg.setCommands(mpc);

        //Speaker Stuff
		//System.setProperty("java.library.path", "path/to/javafx-sdk-11.0.x/lib");
        config = new Configuration(); //Config used for all 
        config.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us"); // Set path to acoustic model.
        config.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict"); // Set path to dictionary.
        config.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin"); // Set language model.
		config.setGrammarPath("file:src"); //Path where grammar file sits
		config.setGrammarName("speak"); //Sets a limited word bank of possible detected words
		config.setUseGrammar(true); //Default value false i think

        //Recognition
		speech = new LiveSpeechRecognizer(config); //Other speech recognizers exist in sphinx but this is the only live one

		speech.startRecognition(true);
		SpeechResult result; //data type of detected words
		
		//Voice Commands
		String caller = "music "; //incase i want to change it

		while ((result = speech.getResult()) != null) {
			String voiceCommand = result.getHypothesis(); //gets result as string
			System.out.println("***Voice Command is " + voiceCommand.toUpperCase() + "***");
			
			//Basic Commands
			if (voiceCommand.equals(caller + "play")) { 
				mpg.playerStatus("pause"); //current status is pause and playerStatus switches that
			}
			if (voiceCommand.equals(caller + "pause")) {
				mpg.playerStatus("play"); //current status is play and playerStatus switches that
			}
			if (voiceCommand.equals(caller + "skip")) {
				mpc.skip();
				mpg.setPlayButton();
			}
			if (voiceCommand.equals(caller + "back")) {
				mpc.previous();
			}
            if (voiceCommand.equals("clear history")) {
				mpc.clear();
			}
			if (voiceCommand.matches("current volume|get volume")) {
				mpc.getVolume();
			}
			if (voiceCommand.length() > 6 && voiceCommand.substring(0, 6).equals("volume")) {
				double t = 0.5;
				switch (voiceCommand) {
					case "volume one": t = 0.1; break;
					case "volume two": t = 0.2; break;
					case "volume three": t = 0.3; break;
					case "volume four": t = 0.4; break;
					case "volume five": t = 0.5; break;
					case "volume six": t = 0.6; break;
					case "volume seven": t = 0.7; break;
					case "volume eight": t = 0.8; break;
					case "volume nine": t = 0.9; break;
					case "volume ten": t = 1; break;
					default: break;
				}

				mpg.setJSlider(t);
				mpc.setVolume(t);
			}
			if (voiceCommand.equals("toggle shuffle")) {
				mpc.shuffle();
			}
			if (voiceCommand.equals("end program")) {
				System.exit(0);
			}
        }
    }
}

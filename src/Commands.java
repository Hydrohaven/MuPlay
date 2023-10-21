package src;
import java.io.*;
import java.util.*;
import javax.swing.JFrame;
import javafx.scene.media.*;

public class Commands implements Runnable {
    private ArrayList<MediaPlayer> playlist = new ArrayList<MediaPlayer>(); //list of MediaPlayers used to play the songs
    private ArrayList<String> songList = new ArrayList<String>(); //list of songs used primarily to grab song and artist name
    //private ArrayList<Integer> history = new ArrayList<Integer>(); //list of MP history primarily used when program is in shuffle (Edit 8/13/22, probably shoulda used a stack)
    private Stack<MediaPlayer> queue = new Stack<MediaPlayer>(); //list of songs that are queued by shuffle
    private Stack<MediaPlayer> history = new Stack<MediaPlayer>(); //list of songs previous played
    
    private MediaPlayer mp; //current MediaPlayer that changes based on currentSong in skip()
    private Scanner sc;
    
    private int csIndex; //current song index
    private double defaultVolume = 0.5;

    private boolean shuffle;
    private boolean firstTime = true; //used when a song is played for the first time (when switching to it)

    private JFrame jf;
    Thread timerThread;

    //***Notes***
    //mp.getCycleDuration() is used to check how long a song is in milliseconds.
    //mp.getStatus() shows the current status of the mediaplayer
    //addSongs() checks folder for folders and files, files are added to the playlist and folders and checked for other files able to be added

    public Commands(JFrame frame, File folder) {
        //Start
        sc = new Scanner(System.in);
        jf = frame;

        //File Chooser
        addSongs(folder);

        addQueue(); //sets queue at random (shuffle)
        mp = queue.pop(); //current song is top of queue
        csIndex = playlist.indexOf(mp); //gets index of current song so i can get its information and use in playSelect
        shuffle = true;
        //nextSong(shuffle);

        //Post-Processing
        timerThread = new Thread(this);
        timerThread.start();
        mp.setVolume(defaultVolume);
        play();
    }

    public void bookmarks() {
        addSongs(null);
        play();
        skip();
        previous();
        playSelect(csIndex);
        pause();
        shuffle();
        run();
    }

    public void addSongs(File folder) {
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                addSongs(fileEntry);
            } else {
                String songName = "";
                playlist.add(new MediaPlayer(new Media(new File(fileEntry.toString()).toURI().toString())));
                songName = fileEntry.getName();
                songName = songName.substring(0, songName.lastIndexOf(".")); //gets rid of file type

                if (songName.indexOf("IZONE") != -1) {
                    songName = songName.substring(0, songName.indexOf("IZ")+2) + "*ONE";
                }
                songList.add(songName);
            }
        }

        System.out.println("Playlist Size: " + playlist.size() + " songs");
    }

    public void addQueue() { //new shuffle method because i am now adding stacks like i always should have (10/07/22)
        //Clears history and queue because fresh start
        history.clear();
        queue.clear();

        for (int i = 0; i < playlist.size(); i++) {
            int rand = (int)(Math.random() * playlist.size());
            if (!queue.contains(playlist.get(rand))) { //if song isn't in queue, add it
                queue.push(playlist.get(rand));
            } else { //if it is in queue, dont add it and subtract i by 1 so
                i--;
            }
        }        
    }

    public void addHistory(MediaPlayer mp) {
        history.push(mp);
    }

    public void start() {
        switch(sc.nextLine()) {
            case "play": play(); break;
            case "pause": pause(); break;
            case "gvol": getVolume(); break;
            case "svol": setVolume(); break;
            case "stop": stop(); break;
            case "end": System.exit(0);
            case "repeat": repeat(); break;
            case "skip": skip(); break;
            case "shuffle": shuffle(); break;
            case "back": previous(); break;
            case "check": System.out.println(mp.getStatus()); start(); break;
            case "clear": history.clear(); start(); break;
            default: System.out.println("Try again"); start(); break;
        }
        
    }

    public ArrayList<String> getPlaylist() {
        return songList;
    }

    public void play() {
        mp.play();
        csIndex = playlist.indexOf(mp);
        String minutes = (int)((mp.getCycleDuration().toSeconds())/60)+"";
        String seconds = Math.round((int)((mp.getCycleDuration().toSeconds())%60))+"";
        if (seconds.length() == 1) {
            seconds = "0"+seconds;
        }

        jf.setTitle(songList.get(csIndex) + " | " + minutes+":"+seconds);

        if (firstTime) {
            //For sweaty moments only, separates song name and artist by "-" but won't always work if i select files that arent named that way
            // String name = songList.get(currentSong).substring(0, songList.get(currentSong).lastIndexOf("-")-1);
            // String artist = songList.get(currentSong).substring(songList.get(currentSong).lastIndexOf("-")+2);
            // System.out.println("Now Playing: " + name + " | By: " + artist + " | Length: " + minutes + ":" + seconds + " |");
            //Edit 11/23/22: I should just implement this because I'm always gonna name my songs like this
            //By doing so I can also sort songs into different artist playlists easier by just calling the artist string
            //New Idea: I can give some songs 

            System.out.println("Now Playing: " + songList.get(csIndex) + " | Length: " + minutes + ":" + seconds + " |");
            firstTime = false;
        }
    }

    public void skip() {
        mp.stop();
        firstTime = true;

        // lastSong = currentSong; //last song is now equal to what WAS the current one
        // currentSong = nextSong; //the current playing song is what WAS the upcoming song
        // nextSong(shuffle); //the upcoming song is now the current one + 1

        history.push(mp);
        mp = queue.pop();

        mp.setVolume(defaultVolume);
        play();
    }

    public void previous() {
        mp.stop();
        firstTime = true;

        // nextSong = currentSong; //next song is what WAS my current song 
        // currentSong = lastSong; //current song is now what WAS the last song i played, the song i skipped/finsihed
        // if (shuffle) {
        //     lastSong = history.indexOf(currentSong);
        // } else {
        //     lastSong = currentSong-1;
        // }

        queue.push(mp);
        mp = history.pop();

        mp.setVolume(defaultVolume);
        play();
    }

    public void playSelect(int index) { //Only used in GUI song selection
        mp.stop();
        firstTime = true;

        // lastSong = currentSong;
        // currentSong = index;
        // nextSong(shuffle);
        //call new shuffle method instead of weird stuff

        addQueue(); //reshuffle queue
        queue.remove(playlist.get(index)); //remove currently selected song from queue so it doesnt play twice
        mp = playlist.get(index);

        mp.setVolume(defaultVolume);
        play();
    }

    public void pause() {
        mp.pause();
        //mp.setStartTime(mp.getCurrentTime()); //Why was this here? I don't see a point since MediaPlayer has a built in play and pause, outside intervention is unecessary
    }

    public void shuffle() {
        if (shuffle) {
            shuffle = false;
            addQueue();
        } else {
            shuffle = true;
            //nextSong(shuffle);
        }
        //start(); //Uncomment when using scanner mode
    }

    

    public void getVolume() {
        System.out.println(mp.getVolume());
    }

    public void setVolume() { //Scanner method
        System.out.println("Type double volume");
        mp.setVolume(sc.nextDouble());
        start();
    }

    public void setVolume(double d) { //Voice method
        mp.setVolume(d);
        defaultVolume = d; //all future songs now are set to this volume 
    }

    public void clear() {
        history.clear();
    }

    public void stop() {
        mp.stop();
    }

    public void repeat() { //Unused
        mp.setCycleCount(-1);
    }

    @Override
    public void run() { //An infinite loop. Every milisecond that timerThread exists, it checks whether the song has ended or not. If so, skip
        long lastTime = System.nanoTime();
        long currentTime;
        long timer = 0;
        
        while (timerThread != null) { //until i find a way to reduce cp usage, having this active brings cpu from 0.2% to 12% :(
            currentTime = System.nanoTime();

            timer += (currentTime - lastTime);
            lastTime = currentTime;

            if (timer >= 1000000000) {
                if (mp.getCurrentTime().equals(mp.getCycleDuration())) {
                    skip();
                }
                timer = 0;
            }
        } //ITS BEEN MADE, ITS FINALLY BEEN MADE AHAHAHAHAHAHAHAHAA HAHAHAHAHAHAHHAA AHAHAHAHAHAHA
    }

    public void printHistory(ArrayList<Integer> arr) {
        for (int i : arr) {
            System.out.print(i + " ");
        }
        System.out.println();
    }
}

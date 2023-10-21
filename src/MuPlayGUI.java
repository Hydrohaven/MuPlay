package src;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;


public class MuPlayGUI implements MouseListener, MouseMotionListener{
    private DefaultListModel<String> listModel = new DefaultListModel<String>();
    private ArrayList<String> playlist = new ArrayList<String>();
    private JList<String> list;
    private int clickCount;
    private Commands cmd;
    private JFrame frame;
    private File folder;
    private JSlider slider;

    //Buttons & Icons
    private JButton playButton;
    private String status = "play";
    private ImageIcon playIcon, pauseIcon, skipIcon, backIcon, blankSpace;

    public MuPlayGUI() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            // UIManager.put("List.focusCellHighlightBorder", BorderFactory.createEmptyBorder());
        } catch (Exception e) { 
            System.err.println("Error: " + e.getMessage()); 
        }

        //Setup
        JPanel panel = new JPanel(new BorderLayout());
        JFileChooser jfc = new JFileChooser();
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setFileHidingEnabled(true);
        jfc.showOpenDialog(null);
        folder = jfc.getSelectedFile();
        listFilesForFolder(folder);
        createImages();

        //JList
        list = new JList<String>(Arrays.copyOf(playlist.toArray(), playlist.size(), String[].class)); 
        list.setModel(listModel);
        for (String str : playlist) {
            listModel.addElement(str);
        }
        list.validate();
        list.addMouseListener(this);
        list.addMouseMotionListener(this);
        list.setFont(new Font("Dialog", Font.PLAIN, 15)); //Font family dialog retains foreign languages, arial does not (Defualt font:Dialog, Bold, 12)
        
        //JScrollPane
        JScrollPane pane = new JScrollPane();
        pane.setViewportView(list);
        list.setLayoutOrientation(JList.VERTICAL);
        panel.add(pane, BorderLayout.CENTER);

        //Buttons
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        //Play
        playButton = new JButton();
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (status) {
                    case "play": playButton.setIcon(playIcon); status = "pause"; cmd.pause(); break; //If song is playing, set icon to play icon and pause the music
                    case "pause": playButton.setIcon(pauseIcon); status = "play"; cmd.play(); break; //If song is paused, set icon to pause icon and play the music
                } //icon is opposite to what is current status since clicking on button brings you to that opposite
            }
        });
        fixButton(playButton);
        playButton.setIcon(pauseIcon);

        //Skip
        JButton skipButton = new JButton();
        skipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmd.skip();
            }
        });
        fixButton(skipButton);
        skipButton.setIcon(skipIcon);

        //Previous
        JButton backButton = new JButton();
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cmd.previous();
            }
        });
        fixButton(backButton);
        backButton.setIcon(backIcon);

        //Blank
        JButton blank = new JButton();
        fixButton(blank);
        blank.setIcon(blankSpace);

        //Button 끝
        c.insets = new Insets(0, 0, 0, 140);
        buttonPanel.add(blank, c);
        c.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(backButton, c);
        c.insets = new Insets(5, 5, 5, 5);
        buttonPanel.add(playButton, c);
        c.insets = new Insets(0, 0, 0, 0);
        buttonPanel.add(skipButton, c);

        //JTextField
        JPanel textPanel = new JPanel(new BorderLayout());
        JTextField text = new JTextField();
        text.setFont(new Font("Dialog", Font.PLAIN, 15));
        text.setPreferredSize(new Dimension(250, 25));
        text.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {                
            }

            @Override
            public void keyPressed(KeyEvent e) {                
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();

                if (/*key == KeyEvent.VK_ENTER*/ key != -1) {
                    listModel.clear();
                    for (String str : playlist) {
                        if (str.toUpperCase().indexOf(text.getText().toUpperCase()) != -1) {
                            listModel.addElement(playlist.get(playlist.indexOf(str)));
                        }
                    }
                    list.validate();
                }
            }
            
        });
        textPanel.add(text, BorderLayout.EAST);

        //Slider Panel
        slider = new JSlider(0, 100, 50);
        slider.setPreferredSize(new Dimension(150, 25)); //default is 200, 25
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                cmd.setVolume(source.getValue()/100.0);
            }
        });
        c.insets = new Insets(0, 140, 0, 0);
        buttonPanel.add(slider, c);

        //Panels
        panel.add(buttonPanel, BorderLayout.PAGE_END); 
        panel.add(textPanel, BorderLayout.NORTH);

        //JFrame
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.setSize(750,600);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(new ImageIcon("images/chaewon 18 icon.png").getImage());
        frame.setVisible(true);
    }

    public void createImages() {
        //Play
        playIcon = new ImageIcon("images/playButton.png");
        playIcon = new ImageIcon(playIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH));

        //Pause
        pauseIcon = new ImageIcon("images/pauseButton.png");
        pauseIcon = new ImageIcon(pauseIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH));

        //Skip
        skipIcon = new ImageIcon("images/skipButton.png");
        skipIcon = new ImageIcon(skipIcon.getImage().getScaledInstance(33, 37, Image.SCALE_SMOOTH));

        //Skip
        backIcon = new ImageIcon("images/backButton.png");
        backIcon = new ImageIcon(backIcon.getImage().getScaledInstance(33, 37, Image.SCALE_SMOOTH));

        //Blank
        blankSpace = new ImageIcon("images/blank.png");
        blankSpace = new ImageIcon(blankSpace.getImage().getScaledInstance(150, 25, Image.SCALE_SMOOTH));
    }

    public void playerStatus(String s) {
        switch (status) { //this kindve confuses me
            case "play": playButton.setIcon(playIcon); status = "pause"; cmd.pause(); break; //If song is playing, set icon to play icon and pause the music
            case "pause": playButton.setIcon(pauseIcon); status = "play"; cmd.play(); break; //If song is paused, set icon to pause icon and play the music
        } //icon is opposite to what is current status since clicking on button brings you to that opposite
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        clickCount++;

        if (clickCount == 2) { //if double clicked, plays song
            cmd.playSelect(playlist.indexOf(list.getSelectedValue()));
            clickCount = 0; //resets counter to 0 to avoid errors
        }

    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        clickCount = 0;
    }
    public void setCommands(Commands c) {
        cmd = c;
    }

    public JFrame getJFrame() {
        return frame; 
    }

    public void setPlayButton() { //used when skipping a paused song since the play button doesnt change
        playButton.setIcon(pauseIcon);
    }

    public void setJSlider(double t) {
        slider.setValue((int)(t*100));
    }

    public File getFolder() {
        return folder;
    }

    public void fixButton(JButton button) {
        button.setBorder(BorderFactory.createEmptyBorder()); //makes button an image, if image is set to it
        button.setContentAreaFilled(false);
    }

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                String songName = fileEntry.getName();
                songName = songName.substring(0, songName.lastIndexOf("."));
                if (songName.indexOf("IZONE") != -1) {
                    songName = songName.substring(0, songName.indexOf("IZ")+2) + "*ONE";
                }
                playlist.add(songName);
            }
        }
    }

    //Extra Methods
    @Override
    public void mouseDragged(MouseEvent e) {
        
    }  

    @Override
    public void mousePressed(MouseEvent e) {
        
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        
    }
    
}
